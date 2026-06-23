# architect-event 批量发消息（OutboxStealer）

## 问题

当前每次事务只发本事务的事件到 MQ，无法利用消息队列的管道吞吐量：

```
事务1: INSERT 5 条 → afterCommit 5次 executor.submit(doPublish)
事务2: INSERT 3 条 → afterCommit 3次 executor.submit(doPublish)
事务3: INSERT 1 条 → afterCommit 1次 executor.submit(doPublish)
```

MQ 碎片化推送，高并发下吞吐量很低。

## 约束

- 事件存储必须绑定事务（ACID 要求），不能脱离到本地缓存或堆外内存
- K8s 无状态部署，不能依赖本地文件
- MQ adapter 层轻量改动：`EventPublisher` 接口加 `publishBatch()` 默认方法，RocketMQ/Pulsar 覆写原生批，其余退化逐条
- **分库分表场景下不能扫全表**，避免低效的跨分片扫描

## 方案

在现有异步发送基础上增加一层 **LinkedBlockingQueue 内存队列**，OutboxStealer 从队列 drain 攒批后统一推 MQ。DB 扫表仅做低频兜底（进程崩溃恢复、队列溢出后的补偿）。

```
改动前:
  beforeCommit → batchInsert arch_event（事务内）
  afterCommit  → forEach: executor.submit(doPublish)  ← 逐条碎片化

改动后:
  beforeCommit → batchInsert arch_event（事务内，不变）
  afterCommit  → entities → LinkedBlockingQueue（入队，微秒级）
  OutboxStealer → drain 队列 → 攒批 500 条或 200ms 超时
                → 按 topic 分组 → publish(List) → MQ
                → 成功后 BatchEventMarker 批量标记

  [低频兜底]
  OutboxStealer → 每 30s 扫 arch_event WHERE state=0 LIMIT 500（单分片内扫）
                → 处理进程崩溃 / 队列满后遗漏的事件
```

## 架构图

```
┌──────────────┐     ┌───────────────────┐     ┌──────────┐
│ beforeCommit │────→│ arch_event (state=0) │    │          │
│  batchInsert │     │                     │     │   DB     │
└──────────────┘     └───────────────────┘     │          │
                            ↑                  └─────┬────┘
                            │ 低频扫表 (30s)           │
┌──────────────┐     ┌─────┴──────────────────┐       │
│ afterCommit  │────→│ LinkedBlockingQueue     │──X    │
│  offer()     │     │ (capacity=65536)       │ 队列满│降级
└──────────────┘     └────────┬───────────────┘       │
                              │ drain                 │
                       ┌──────▼──────────┐            │
                       │  OutboxStealer  │────────────┘
                       │  · 攒批 500条   │
                       │  · 超时 200ms   │
                       │  · 按topic分组  │
                       └──────┬──────────┘
                              │ publish(List)
                       ┌──────▼──────────────┐
                       │ MessageQueuePublisher│
                       │  · 按topic分组submit │
                       │  · doPublish逐条发MQ │
                       └──────────────────────┘
```

## 改动清单

### 1. `EventPublisherSynchronization` — afterCommit 改为入队

**文件：** `architect-event/architect-event-boot-starter/src/main/java/com/cloud/arch/event/publisher/EventPublisherSynchronization.java`

- `beforeCommit()` — 不变
- `afterCommit()` — 不再调用 `queuePublisher.publish()`，改为 `queuePublisher.enqueue(entities)`
- `afterCompletion()` — 不变

### 2. `MessageQueuePublisher` — 新增内存队列 + enqueue/drain 方法

**文件：** `architect-event/architect-event-commons/src/main/java/com/cloud/arch/event/core/publish/MessageQueuePublisher.java`

新增：

```java
// 共享队列（stealer 跨事务 drain）
private final BlockingQueue<PublishEventEntity> eventQueue;

// 构造函数中初始化
this.eventQueue = new LinkedBlockingQueue<>(queueCapacity);

// afterCommit 调用（入队，非阻塞）
public void enqueue(List<PublishEventEntity> entities) {
    entities.forEach(entity -> {
        if (!eventQueue.offer(entity)) {
            log.warn("event queue full, event[{}] will be recovered by stealer db scan", entity.getId());
        }
    });
}

// stealer 调用（drain 攒批）
public List<PublishEventEntity> drainQueue(int maxSize, long timeoutMs) {
    List<PublishEventEntity> batch = new ArrayList<>(maxSize);
    // 先 drainTo 一次性拿出所有
    eventQueue.drainTo(batch, maxSize);
    if (batch.size() >= maxSize) return batch;
    // 不够 maxSize 则等 timeoutMs
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (batch.size() < maxSize) {
        long wait = deadline - System.currentTimeMillis();
        if (wait <= 0) break;
        PublishEventEntity entity = eventQueue.poll(wait, TimeUnit.MILLISECONDS);
        if (entity == null) break;
        batch.add(entity);
    }
    return batch;
}
```

`publish(List)` 改为按 topic 分组提交：

```java
public void publish(List<PublishEventEntity> entities) {
    if (CollectionUtils.isEmpty(entities)) return;
    Map<String, List<PublishEventEntity>> grouped = entities.stream()
        .collect(Collectors.groupingBy(PublishEventEntity::getName));
    grouped.values().forEach(batch -> {
        try {
            executorService.submit(() -> batch.forEach(this::doPublish));
        } catch (RejectedExecutionException e) {
            batch.forEach(entity -> {
                statsManager.statsCounter(entity.getName()).recordPublishFallbackSync();
                doPublish(entity);
            });
        }
    });
}
```

属性中 `publishCachedEventSize` 默认值改为 65536（原 8192），作为内存队列容量。

### 3. 新建 `OutboxStealer`

**文件（新建）：** `architect-event/architect-event-boot-starter/src/main/java/com/cloud/arch/event/publisher/OutboxStealer.java`

```java
@Slf4j
public class OutboxStealer implements DisposableBean, SmartInitializingSingleton {

    private final MessageQueuePublisher          publisher;
    private final IDomainEventRepository         repository;
    private final ScheduledExecutorService       scheduler;
    private final PublishEventProperties.Stealer props;

    public OutboxStealer(MessageQueuePublisher publisher,
                         IDomainEventRepository repository,
                         PublishEventProperties properties) {
        this.publisher = publisher;
        this.repository = repository;
        this.props = properties.getStealer();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            Threads.threadFactory("outbox-stealer"));
    }

    /**
     * 主循环：从内存队列 drain 攒批，低延迟路径
     */
    private void run() {
        List<PublishEventEntity> batch = publisher.drainQueue(
            props.getBatchSize(), props.getDrainTimeoutMs());
        if (!batch.isEmpty()) {
            publisher.publish(batch);
        }
    }

    /**
     * 兜底：低频扫 DB 恢复遗漏事件（进程崩溃 / 队列满）
     */
    private void recovery() {
        long olderThanMs = System.currentTimeMillis() - props.getMinAge().toMillis();
        List<PublishEventEntity> batch = repository.queryInitialized(
            props.getBatchSize(), olderThanMs);
        if (!batch.isEmpty()) {
            publisher.publish(batch);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 主线程：高频 drain 队列
        Thread drainThread = new Thread(Threads.threadFactory("outbox-drain"), this::run);
        drainThread.setDaemon(true);
        drainThread.start();
        // 兜底：低频扫 DB
        scheduler.scheduleWithFixedDelay(this::recovery,
            props.getRecoveryInterval().toMillis(),
            props.getRecoveryInterval().toMillis(),
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        this.scheduler.shutdown();
    }
}
```

**走两个路径**

| 路径 | 数据源 | 频率 | 用途 |
|------|--------|------|------|
| drain 线程 | LinkedBlockingQueue | 持续（阻塞 drain） | 正常路径，低延迟 |
| recovery 线程 | DB SELECT state=0 | 30s | 兜底，进程崩溃恢复 |

### 4. `IDomainEventRepository` 新增查询方法

**文件：** `architect-event/architect-event-commons/src/main/java/com/cloud/arch/event/storage/IDomainEventRepository.java`

```java
List<PublishEventEntity> queryInitialized(int limit, long olderThanMs);
```

### 5. `JdbcDomainEventRepository` 新增 SQL + 实现

**文件：** `architect-event/architect-event-storage/architect-event-storage-jdbc/src/main/java/com/cloud/arch/event/JdbcDomainEventRepository.java`

```sql
select id,name,filter,delay,event,shard_key,state,version,gmt_create
from arch_event
where state=0 and gmt_create < :older_than
order by gmt_create asc limit :limit
```

### 6. `PublishEventProperties` 新增 Stealer 配置

**文件：** `architect-event/architect-event-boot-starter/src/main/java/com/cloud/arch/event/props/PublishEventProperties.java`

```java
@Data
public static class Stealer {
    /** 内存队列每次 drain 最大条数 */
    private int      batchSize        = 500;
    /** drain 等待超时(ms) */
    private long     drainTimeoutMs  = 200;
    /** DB 兜底扫描间隔 */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration recoveryInterval = Duration.ofSeconds(30);
    /** 最小事件年龄（留给 BatchEventMarker 窗口） */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration minAge           = Duration.ofSeconds(5);
}

private Stealer stealer = new Stealer();
```

### 7. `CloudEventAutoConfiguration` 注册 stealer

**文件：** `architect-event/architect-event-boot-starter/src/main/java/com/cloud/arch/event/boot/CloudEventAutoConfiguration.java`

在 `EventPublisherConfiguration` 中新增：

```java
@Bean
public OutboxStealer outboxStealer(MessageQueuePublisher queuePublisher,
                                    IDomainEventRepository repository,
                                    PublishEventProperties properties) {
    return new OutboxStealer(queuePublisher, repository, properties);
}
```

---

## 附录：各 MQ 批量发送能力调研

> **注意：实施时先移除 ONS 模块**（见 `[ONS-REMOVAL]` 任务），以下调研按移除后的 4 种 MQ 展开。

调研结论：4 种 MQ 批量能力差异大，需在 `EventPublisher` 接口层统一抽象 + 各实现退化处理。

### 调研明细

| MQ | 原生批量 API | 机制 | 可批？ | 改动量 |
|---|---|---|---|---|
| **Kafka** | `send(ProducerRecord)` 单条 | `KafkaProducer` 内部 accumulator，`linger.ms` + `batch.size` 控制攒批窗口，多次 `send()` 自动合并为一次 TCP 请求 | **隐式批** | 低，仅暴露 config |
| **Pulsar** | `ProducerBuilder.enableBatching()` | 生产者级配置，需 `sendAsync()` 才能让批量窗口计时生效；`send()` 同步等待会立即 flush | **需改 sendAsync** | 中，加 config + 改 send 调用 |
| **RocketMQ 5.x** | `DefaultMQProducer.send(Collection<Message>)` | **单次网络调用发送多条 Message**，同 topic 同 tag 合并到一条 remoting 命令 | **原生批** | 中，需重写 `publishBatch()` |
| **RabbitMQ** | `BatchingRabbitTemplate`（Spring 层） | 多条消息打包到一个 AMQP 消息体中，**消费端需配套解包** | **模拟批** | 高，替换 Template + 消费端改造 |

### 接口层设计

```java
public interface EventPublisher {
    // 现有单条（不变）
    void publish(EventMessage message);

    // 新增批量（默认退化逐条，支持原生批的 MQ 覆写）
    default void publishBatch(List<EventMessage> messages) {
        messages.forEach(this::publish);
    }
}
```

### 各实现处理策略

```java
// Kafka: 不覆写 publishBatch，靠 producer config 隐式批
// Pulsar: 覆写 publishBatch，内部 sendAsync() + CompletableFuture.allOf().join()
// RocketMQ: 覆写 publishBatch，调 send(Collection<Message>)
// RabbitMQ: 可选 BatchingRabbitTemplate，默认退化
// ONS: 退化逐条 send(Message)
```

| MQ | publishBatch 策略 | 额外配置 |
|---|---|---|
| Kafka | 默认退化（逐条 send，producer 内部自动批） | 暴露 `linger.ms`、`batch.size` 到 `KafkaEventProperties` |
| Pulsar | 覆写：`sendAsync()` + barrier wait | `PulsarMqProperties` 加 `batchingMaxPublishDelay`、`batchingMaxMessages` |
| RocketMQ | 覆写：`send(Collection<Message>)` 单次网络调用 | 利用已有 `maxMessageSize` 限制总 payload |
| RabbitMQ | 可选 `BatchingRabbitTemplate`，需消费端配套 | `RabbitmqProperties` 加 batching 块 |
| ONS | **退化逐条**，无批处理路径 | 无 |

### MessageQueuePublisher.publish(List) 适配

```java
public void publish(List<PublishEventEntity> entities) {
    if (CollectionUtils.isEmpty(entities)) return;
    // 按 topic 分组
    Map<String, List<PublishEventEntity>> grouped = entities.stream()
        .collect(Collectors.groupingBy(PublishEventEntity::getName));
    grouped.forEach((topic, batch) -> {
        List<EventMessage> messages = batch.stream()
            .map(PublishEventEntity::build).toList();
        try {
            executorService.submit(() -> eventPublisher.publishBatch(messages));
        } catch (RejectedExecutionException e) {
            // 降级同步，仍走 batch
            eventPublisher.publishBatch(messages);
        }
    });
}
```

每组同 topic 事件调一次 `publishBatch()`：
- Kafka → 默认退化，内部逐条 `send()` 靠 producer accumulator 自动聚合
- RocketMQ → 一次 `send(Collection)` 网络调用
- Pulsar → 配置了 batching 则 `sendAsync()` 聚合
- ONS/RabbitMQ → 退化逐条

---

## 影响面

| 维度 | 影响 |
|------|------|
| 事务 | `beforeCommit` 不变，ACID 保障不动 |
| 延迟 | 正常路径由 `drainTimeoutMs` 决定（默认 200ms），几乎无感知 |
| 分库分表 | 正常路径零扫表，DB 扫描仅 30s 低频兜底，单分片内 limit 500 |
| 队列溢出 | 非阻塞 offer 失败后静默丢弃，由 recovery 兜底扫 DB 恢复 |
| 进程崩溃 | 队列内未发的事件丢失，recovery 每 30s 从 DB 恢复 |
| MQ 吞吐 | 500 条一组按 topic 分组提交，充分利用 MQ 管道 |

## 验证

1. `mvn compile` 通过
2. 配置 `com.cloud.event.publisher.enable=true`，启动应用
3. 触发事务发布事件 → 事件入队列 → drain 攒批 → MQ 接收确认
4. 模拟进程 kill → 重启后 recovery 扫 DB 恢复未发事件
5. 压测对比改前后的 MQ 吞吐量
