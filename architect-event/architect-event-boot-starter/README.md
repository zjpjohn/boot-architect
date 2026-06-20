# architect-event-boot-starter 领域事件组件使用文档

## 1. 概述

本组件是 [architect-event](../architect-event-commons/README.md) 领域事件框架的 Spring Boot 自动配置模块，通过事务同步器 + 消息队列 + 幂等检查三机制，为跨服务业务事件提供完整的**发布—投递—消费—补偿**闭环。

### 核心特性

- **事务驱动发布**：基于 `TransactionSynchronization` 接口，事务提交「前」持久化、「后」投递消息队列，杜绝不一致
- **全链路幂等**：消费端利用 `INSERT IGNORE` 原子去重，并发重复消息由 InnoDB 唯一索引自动排队
- **多队列适配**：Kafka / RocketMQ 5.x / RocketMQ ONS / Pulsar / RabbitMQ，按类路径条件自动装配
- **双存储后端**：JDBC（MySQL 等）和 RocksDB 两种事件存储可选，支持分库分表
- **失败补偿**：内置定时扫描任务 + 分布式互斥锁，自动捞取失败事件重试
- **SpEL 分片**：支持 SpEL 表达式提取分片键和幂等 Key，适配分库分表场景
- **轻量侵入**：发布端仅需 `DomainEventPublisher.publish(event)` 一行静态调用

### 架构简图

```
DomainEventPublisher.publish(event)                  ← 业务代码调用
        ↓
TransactionSynchronization                          ← Spring 事务同步
  ├── beforeCommit: initStorage() 持久化             ← 事务内，保证不丢
  └── afterCommit:  publish()     入队               ← 事务提交后，非阻塞入队
        ↓
BufferedTrigger drain → BatchMessagePublisher       ← 攒批按 topic 分组异步发送
        ↓
Kafka / RocketMQ / Pulsar / RabbitMQ                ← 消息队列（原生异步 API）
        ↓
SubscriberProcessor → EventSubscribeHandler         ← 消费端
  ├── isProcessed()  INSERT IGNORE 幂等检查          ← 原子去重
  ├── publishEvent()  业务处理                       ← Spring 事件驱动
  └── markProcessed() COMMIT/ROLLBACK               ← 标记完成
        ↓
IdempotentCleanScheduler                            ← 定时清理过期幂等记录
```

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-event-boot-starter</artifactId>
</dependency>
```

发布端和订阅端通过配置按需启用，无需引入额外依赖。

### 2.2 最小配置 — 发布端

```yaml
com:
  cloud:
    event:
      publisher:
        enable: true                           # 启用事件发布
```

> 发布端需要存在 `DataSource` Bean，如果项目未配置数据源，需额外引入 `spring-boot-starter-jdbc`。

### 2.3 最小配置 — 订阅端

```yaml
com:
  cloud:
    event:
      subscriber:
        enable: true
      rocket:
        v5x:
          name-srv: 127.0.0.1:9876
```

### 2.4 发布领域事件

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder(Order order) {
        // 业务逻辑
        orderRepository.save(order);

        // 发布领域事件 — 事务提交后自动投递到消息队列
        DomainEventPublisher.publish(new OrderCreatedEvent(order));

        // 指定分库分表 shardingKey（可选）
        DomainEventPublisher.shardingKey(order.getUserId());
    }
}
```

### 2.5 订阅领域事件

```java
@Component
public class OrderEventListener {

    /**
     * 通过 @EventListener + 配置即可指定订阅参数
     */
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // 业务处理：发送通知、更新统计等
    }
}
```

---

## 3. 发布端详解

### 3.1 工作流程

```
@Transactional 方法中调用 DomainEventPublisher.publish(event)
  → 注册 TransactionSynchronization（仅首次）
  → 事件暂存 ThreadLocal

事务 commit 前:
  → beforeCommit: EventPublisherSynchronization
    ├── 远程事件 → eventRepository.initialize(entities) 持久化到 JDBC/RocksDB
    └── 本地事件 → ApplicationContext.publishEvent(event) 直接消费

事务 commit 后:
  → afterCommit: BatchMessagePublisher.publish(entities)
    └── 实体非阻塞入队 BufferedTrigger 内部队列
      → trigger 按 batchSize + drainTimeout 攒批 drain
      → processBatch: 按 topic 分组 → eventPublisher.publishBatch(messages)
      → 每条 message 独立 CompletableFuture 回调:
        ├── 成功 → BatchEventMarker.markSucceeded(entity)
        └── 失败 → BatchEventMarker.markFailed(entity)

事务 complete:
  → afterCompletion: DomainEventPublisher.clear() 清理 ThreadLocal
```

### 3.2 DomainEventPublisher API

| 方法 | 说明 |
|------|------|
| `publish(Object event)` | 发布领域事件，自动识别本地/远程事件 |
| `shardingKey(String key)` | 设置当前事务的分片键，影响后续发布事件的 shardKey |

### 3.3 本地事件 vs 远程事件

| 事件类型 | 传播范围 | 投递方式 |
|---------|---------|---------|
| 本地事件 | 当前 JVM | Spring ApplicationEvent 同步发布 |
| 远程事件 | 跨服务 | 消息队列异步投递 |
| `GenericEvent` | 跨服务（无 Java 类型约束） | 消息队列，JSON 序列化 |

远程事件由注解元数据决定，引入消息队列依赖后，`PublishEvent` 中的 `isLocal()` 返回 false 即为远程事件。

### 3.4 GenericEvent — 泛化事件

当跨服务通信不共享 Java 类型时，通过 `GenericEvent` 接口发布：

```java
// 最小用法：仅指定事件内容与 topic
DomainEventPublisher.publish(GenericEvent.create(orderJson, "order.created"));

// 完整用法：指定 filter 与分片键
DomainEventPublisher.publish(GenericEvent.create(orderJson, "order.created", "created", userId));
```

`GenericEvent` 接口还提供了 `filter()`、`shardingKey()`、`delay()`、`timeUnit()`、`bizGroup()` 等默认方法，可通过构造 `PublishGenericEvent` 实例按需覆盖。

---

## 4. 订阅端详解

### 4.1 工作流程

```
消息队列 Listener 收到消息
  → SubscriberProcessor 反序列化消息
  → EventSubscribeHandler.handle(eventKey, event, metadata)
    ├── 构建 EventIdempotent（SpEL 提取分片键 + 幂等 Key）
    ├── idempotentChecker.isProcessed(idempotent)
    │     └── INSERT IGNORE → 1 行 = 未处理 / 0 行 = 已处理（跳过）
    ├── ApplicationContextHolder.publishEvent(event)
    └── idempotentChecker.markProcessed(idempotent, throwable)
          ├── 成功 → markSuccess() → COMMIT
          └── 失败 → markFailed()  → ROLLBACK + DELETE
```

### 4.2 SubscribeEventMetadata 配置

```java
@EventListener
public void onOrderCreated(OrderCreatedEvent event) {
    // 通过元数据注解配置订阅参数（具体映射由各 SubscriberProcessor 实现）
}

// 等价的手动配置：
SubscribeEventMetadata metadata = new SubscribeEventMetadata("order_event", OrderCreatedEvent.class)
    .group("order-consumer-group")
    .filter("created")
    .sharding("#event.userId")     // 分片字段 SpEL
    .key("#event.orderId");        // 幂等 Key SpEL
```

### 4.3 SpEL 表达式

| 配置项 | SpEL 变量 | 示例 | 说明 |
|--------|----------|------|------|
| `sharding` | `#event` 引用事件对象 | `#event.userId` | 提取分库分表字段 |
| `key` | `#event` 引用事件对象 | `#event.orderId` | 提取幂等去重 Key |

表达式错误时降级为空白值，不阻塞消费流程。

---

## 5. 幂等机制

### 5.1 核心原理

```
消息 A 到达 → INSERT IGNORE INTO arch_event_idempot(name, filter, event_key, shard_key)
               ├── affected=1 → 首次处理，继续
               └── affected=0 → 重复消息，跳过

处理成功   → COMMIT（INSERT 生效）
处理失败   → ROLLBACK + DELETE key（下次重试重新处理）
```

### 5.2 并发安全

| 场景 | InnoDB 行为 | 结果 |
|------|-----------|------|
| 两个线程同时消费同一条消息 | 线程 B 的 INSERT 被唯一索引阻塞 | A 提交后 B 返回 0 行，跳过 |
| A 处理失败回滚 | A 回滚时 DELETE key | B 接管控 INSERT 返回 1 行 |
| A 处理超时 | 事务未提交，唯一索引锁持有 | B 被阻塞直到 A 超时回滚或提交 |

唯一索引字段：`(name, filter, event_key, shard_key)`。

### 5.3 幂等表结构

```sql
CREATE TABLE arch_event_idempot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    filter      VARCHAR(64)  NOT NULL DEFAULT '',
    event_key   VARCHAR(256) NOT NULL,
    shard_key   VARCHAR(128) NOT NULL DEFAULT '',
    gmt_create  DATETIME     NOT NULL,
    UNIQUE KEY uk_event (name, filter, event_key, shard_key)
);
```

### 5.4 幂等记录清理

```yaml
com:
  cloud:
    event:
      subscriber:
        before: 2d          # 回收 2 天前的幂等记录
        period: 4d          # 每 4 天执行一次
        initial-delay: 10s  # 启动后延迟 10 秒首次执行
        mutex:
          ttl: 30s          # 分布式锁过期时间
          transition: 15s   # 锁续期间隔
```

---

## 6. 消息队列集成

### 6.1 支持的队列

| 消息队列 | 配置前缀 | 条件 |
|---------|---------|------|
| Kafka | —（复用 `spring.kafka.*`） | 类路径存在 `KafkaEventProperties` |
| RocketMQ 5.x | `com.cloud.event.rocket.v5x` | `name-srv` 配置 |
| RocketMQ ONS | `com.cloud.event.rocket.ons` | `access-key`、`secret-key`、`ons-address` 配置 |
| Pulsar | `com.cloud.event.pulsar` | `enpoints` 配置 |
| RabbitMQ | `com.cloud.event.rabbit` | 类路径存在 `RabbitmqProperties` |

### 6.2 RocketMQ 5.x 配置示例

```yaml
com:
  cloud:
    event:
      publisher:
        enable: true
      subscriber:
        enable: true
      rocket:
        v5x:
          name-srv: 127.0.0.1:9876
          publisher:
            group: order-producer-group
          subscriber:
            group: order-consumer-group
```

> topic 由事件元数据动态指定，非全局配置。

### 6.3 Kafka 配置示例

```yaml
spring:
  kafka:
    bootstrap-servers: 127.0.0.1:9092
com:
  cloud:
    event:
      publisher:
        enable: true
      subscriber:
        enable: true
```

> Kafka 连接和 topic 配置复用 `spring.kafka.*`，无需 `com.cloud.event.kafka.*` 额外配置。

### 6.4 RabbitMQ 配置示例

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
com:
  cloud:
    event:
      publisher:
        enable: true
      subscriber:
        enable: true
      rabbit:
        producer:
          exchange: order.exchange
```

> 连接信息（`host`/`port`/`username`）复用 `spring.rabbitmq.*`；routing key 为事件名动态指定。

---

## 7. 事件存储

### 7.1 JDBC 存储

默认存储方案，利用业务数据库保证事件持久化的 ACID 特性。

```yaml
com:
  cloud:
    event:
      publisher:
        enable: true
        jdbc:
          batch: 20                  # 每次扫描捞取条数
          max-version: 10            # 最大重试次数
          initial-delay: 10s         # 补偿任务启动延迟
          period: 2m                 # 补偿任务执行间隔
```

**消息主表 `arch_event`：**

```sql
CREATE TABLE arch_event (
    id          BIGINT       NOT NULL COMMENT '事件ID',
    name        VARCHAR(128) NOT NULL COMMENT '事件名称',
    filter      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '事件过滤标签',
    delay       BIGINT       NOT NULL DEFAULT 0 COMMENT '延迟发送时间(ms)',
    event       TEXT         NOT NULL COMMENT '事件内容(JSON)',
    shard_key   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '分片键',
    state       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待发送, 1-成功, 2-失败',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    publish_time BIGINT      NULL COMMENT '发布时间戳',
    gmt_create  BIGINT       NOT NULL COMMENT '创建时间戳',
    PRIMARY KEY (id),
    KEY idx_state_gmt (state, gmt_create)
) COMMENT='领域事件消息表';
```

**补偿记录表 `arch_event_compen`：**

```sql
CREATE TABLE arch_event_compen (
    id          BIGINT       NOT NULL COMMENT '补偿记录ID',
    event_id    BIGINT       NOT NULL COMMENT '关联arch_event.id',
    shard_key   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '分片键',
    start_time  BIGINT       NOT NULL COMMENT '补偿开始时间',
    taken       BIGINT       NOT NULL COMMENT '补偿耗时(ms)',
    fail_msg    VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    gmt_create  BIGINT       NOT NULL COMMENT '创建时间戳',
    PRIMARY KEY (id),
    KEY idx_event_id (event_id)
) COMMENT='领域事件补偿记录表';
```

**幂等表 `arch_event_idempot`：**

```sql
CREATE TABLE arch_event_idempot (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '事件名称',
    filter      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '过滤标签',
    event_key   VARCHAR(256) NOT NULL COMMENT '幂等Key',
    shard_key   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '分片键',
    gmt_create  DATETIME     NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_event (name, filter, event_key, shard_key)
) COMMENT='领域事件幂等表';
```

**补偿流程：**

```
JdbcCompensateEventScheduler
  → 分布式锁获取执行权
  → 扫描 arch_event 表中 state<>1 的记录（乐观锁 version 控制并发）
  → JdbcCompensateProcessor.processBatch(entities)
    ├── 按 topic 分组 → eventPublisher.publishBatch(messages) 批量异步投递
    ├── 每条 future 回调中: 成功 → markSucceeded / 失败 → markFailed
    └── allOf(chained).whenComplete → eventRepository.batchCompensate(audits) 批量写入补偿审计
```

**核心 SQL 说明：**

| SQL | 说明 |
|-----|------|
| `INSERT INTO arch_event` | `beforeCommit` 批量写入，业务事务内保证不丢 |
| `UPDATE state=1, version=version+1 WHERE id=? AND version=?` | 乐观锁 CAS 更新，防止重复标记（BatchEventMarker 批量执行） |
| `SELECT ... WHERE state<>1 AND version<maxVersion` | 补偿扫描，按 version 升序取，防止死循环 |
| `INSERT INTO arch_event_compen (batch)` | 补偿审计记录 batch 写入，补偿批次所有 future 完成后一次性插入 |

### 7.2 RocksDB 存储

适用于无 MySQL 依赖或追求极高性能的场景，通过 HTTP 远程补偿。

```yaml
com:
  cloud:
    event:
      publisher:
        enable: true
        rocksdb:
          event-path: /data/event/storage
          http-port: 9099
```

---

## 8. 异常分类处理

`AbstractIdempotentChecker` 支持按异常类型分类处理，通过 `idempotentFor` 和 `retryFor` 字段配置：

```java
// 配置：特定异常视为"已处理"（不做重试）
checker.setIdempotentFor(BusinessValidationException.class);

// 配置：特定异常需要重试
checker.setRetryFor(TimeoutException.class);
```

| 配置 | 行为 |
|------|------|
| `idempotentFor` | 匹配的异常 → `markSuccess()`，不重试 |
| `retryFor` | 匹配的异常 → `markFailed()`，等待补偿重试 |
| 其他异常 | → `markFailed()`，等待补偿重试 |
| 无异常 | → `markSuccess()` |

---

## 9. 配置参考

### 9.1 全部配置项

**发布端 — 核心配置**（前缀 `com.cloud.event`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `publisher.enable` | false | 启用事件发布端 |
| `publisher.batch.batch-size` | 20 | 每次 drain 攒批最大条数 |
| `publisher.batch.drain-timeout` | 200 | drain 等待超时(ms) |
| `publisher.batch.queue-capacity` | 65536 | 内存队列容量 |
| `publisher.marker.batch-size` | 100 | 状态标记单次 batchUpdate 最大条数 |
| `publisher.marker.interval` | 200 | 标记攒批刷新间隔(ms) |
| `metric.enabled` | false | 启用 Micrometer 指标采集 |

**发布端 — JDBC 存储与补偿**（前缀 `com.cloud.event.publisher.jdbc`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `batch` | 20 | 每次扫描捞取条数 |
| `max-version` | 10 | 最大重试次数（超过则进入死信） |
| `before` | 1m | 扫描多久前的事件 |
| `range` | 7d | 扫描时间范围 |
| `initial-delay` | 10s | 补偿任务启动延迟 |
| `period` | 2m | 补偿任务执行间隔 |
| `mutex.initial-delay` | 5s | 补偿锁初始延迟 |
| `mutex.ttl` | 30s | 补偿锁过期时间 |
| `mutex.transition` | 15s | 补偿锁续期间隔 |
| `clean-succeed.retain-days` | 7 | 成功事件保留天数 |
| `clean-succeed.batch-size` | 1000 | 每次清理批大小 |
| `clean-succeed.initial-delay` | 10s | 清理任务启动延迟 |
| `clean-succeed.period` | 1h | 清理任务间隔 |
| `dead-letter.batch` | 10 | 死信归档批量 |
| `dead-letter.before` | 1m | 死信归档 before |
| `dead-letter.range` | 7d | 死信归档 range |
| `dead-letter.initial-delay` | 30s | 死信归档启动延迟 |
| `dead-letter.period` | 30m | 死信归档间隔 |

**发布端 — RocksDB 存储**（前缀 `com.cloud.event.publisher.rocksdb`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `event-path` | — | RocksDB 数据目录 |

**订阅端**（前缀 `com.cloud.event`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `subscriber.enable` | false | 启用事件订阅端 |
| `subscriber.before` | 2d | 回收多久前的幂等记录 |
| `subscriber.initial-delay` | 10s | 首次清理延迟 |
| `subscriber.period` | 4d | 清理间隔周期 |
| `subscriber.mutex.initial-delay` | 5s | 分布式锁初始延迟 |
| `subscriber.mutex.ttl` | 30s | 分布式锁过期时间 |
| `subscriber.mutex.transition` | 15s | 分布式锁续期间隔 |

### 9.2 队列专属配置

**RocketMQ 5.x**（前缀 `com.cloud.event.rocket.v5x`）

| 配置项 | 说明 |
|--------|------|
| `name-srv` | NameServer 地址 |
| `access-key` / `secret-key` | ACL 认证 |
| `publisher.group` | 生产者组名 |

**RocketMQ ONS**（前缀 `com.cloud.event.rocket.ons`）

| 配置项 | 说明 |
|--------|------|
| `access-key` / `secret-key` | 阿里云 AccessKey/SecretKey |
| `ons-address` | ONS 接入点 |

**Kafka** — 无需 `com.cloud.event.kafka.*` 额外配置，连接信息复用 `spring.kafka.bootstrap-servers`。

**Pulsar**（前缀 `com.cloud.event.pulsar`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enpoints` | — | Pulsar 端点 |
| `publisher.enable-batching` | true | 客户端自动攒批 |
| `publisher.batching-max-messages` | 1000 | 每批最大消息数 |
| `publisher.batching-max-publish-delay` | 10 | 批发送最大等待(ms) |
| `publisher.send-timeout` | 30 | 发送超时(s) |
| `publisher.max-pending-messages` | 1000 | 待发送最大消息数 |

**RabbitMQ**（前缀 `com.cloud.event.rabbit`，连接信息复用 `spring.rabbitmq.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `producer.exchange` | "" | 生产者交换机 |

---

## 10. 扩展点

| 接口 | 说明 | 默认实现 |
|------|------|---------|
| `IdempotentChecker` | 幂等检查策略 | `TransactionIdempotentChecker` |
| `IDomainEventRepository` | 事件存储仓储 | JDBC 或 RocksDB |
| `EventCodec` | 事件编解码 | `FastJson2EventCodec` |
| `EventPublisher` | 消息队列发布器 | 按队列类型选择 |
| `SubscribeHandler` | 事件消费处理器 | `EventSubscribeHandler` |

### 自定义示例

```java
@Component
public class RedisIdempotentChecker extends AbstractIdempotentChecker {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean doProcessed(EventIdempotent idempotent) {
        String key = buildKey(idempotent);
        return Boolean.FALSE.equals(redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofDays(7)));
    }

    @Override
    public void markSuccess(EventIdempotent idempotent) {
        // Redis key 已设置，无需额外操作
    }

    @Override
    public void markFailed(EventIdempotent idempotent) {
        redisTemplate.delete(buildKey(idempotent));
    }

    @Override
    public void garbageClean(LocalDateTime before) {
        // Redis TTL 自动过期，无需清理
    }
}
```

---

## 11. 最佳实践

### 11.1 事件设计原则

- **不可变性**：事件对象是所有字段 final，不提供 setter
- **自描述性**：事件名清晰表达语义（`OrderCreated` 而非 `OrderEvent`）
- **最小化**：事件仅包含消费端必需的数据，不传递完整聚合根

### 11.2 事务边界

```java
// ✅ 正确：发布在事务内
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    DomainEventPublisher.publish(new OrderCreatedEvent(order));
}

// ❌ 错误：发布在事务外
public void createOrder(Order order) {
    orderRepository.save(order);
    DomainEventPublisher.publish(new OrderCreatedEvent(order)); // 事务未注册，抛异常
}
```

### 11.3 幂等 Key 设计

```
推荐格式：{事件名}:{业务ID}
示例：    order.created:ORD20260509001

对应配置：
  key = "#event.orderId"  → 自动拼接为 "order.created:ORD20260509001"
```

### 11.4 攒批参数调优

```
queueCapacity > 峰值 TPS × drainTimeout / 1000

示例：峰值 500 TPS，drainTimeout=200
  → 每 200ms 攒批 ≈ 100 条 → batchSize 设 100~150
  → queueCapacity = 500 × 0.2 × 10 倍余量 = 1000
  → marker.batchSize 设 100~200，interval 设 200~500ms
```

- `publisher.batch.batch-size`：单次 drain 最大条数，太小浪费攒批效果，太大增加单次发送延迟
- `publisher.batch.drain-timeout`：攒批窗口，到达即排空；配合 batchSize 先到先发
- `publisher.marker.batch-size`：状态标记批量写入大小，减少逐条 UPDATE
- `publisher.marker.interval`：标记攒批刷新间隔，延迟越小标记越实时

### 11.5 分库分表场景

```java
@Transactional
public void createOrder(Order order) {
    // 设置分片键，影响事件的 shard_key 字段
    DomainEventPublisher.shardingKey(order.getUserId());
    orderRepository.save(order);
    DomainEventPublisher.publish(new OrderCreatedEvent(order));
}
// 事件自动带上 shard_key，幂等表按分片键路由
```

---

## 12. 常见问题

**Q: 为什么用 `INSERT IGNORE` 而不是分布式锁？**

分布式锁存在超时不可控、脑裂、锁丢失等风险。`INSERT IGNORE` + 唯一索引利用数据库原子性，不依赖外部组件，可靠性更高。并发重复消息由 InnoDB gap lock 自动排队等待当前事务提交，行为可预测。

**Q: 事务回滚了，事件还会发吗？**

不会。`EventPublisherSynchronization` 仅在 `afterCommit` 中投递，事务回滚不会触发。同时 `beforeCommit` 持久化也在事务内，回滚后事件记录一并回滚。

**Q: 消息队列投递失败怎么办？**

每条消息的 `CompletableFuture` 回调中，失败时通过 `BatchEventMarker.markFailed(entity)` 标记；补偿定时任务 `JdbcCompensateEventScheduler` 定期扫描失败记录，按 topic 分组后调用 `publishBatch` 批量重试，完成后批量写入补偿审计记录。

**Q: 如何切换存储后端？**

引入对应依赖并配置即可，JDBC 和 RocksDB 互斥（共享 `IDomainEventRepository` Bean），`@Primary` 注解控制优先级。

**Q: 幂等表数据膨胀？**

`IdempotentCleanScheduler` 定时删除 `gmt_create < now - before` 的过期记录，默认 2 天 + 每 4 天清理。分布式锁保证多节点仅一个执行。

**Q: 同一事务内发布多个事件，先后顺序是否有保证？**

有。`ThreadLocal<LinkedList>` 保证事件按 publish 调用顺序存储，`beforeCommit` 按序持久化，`afterCommit` 按序入队 BufferedTrigger。攒批 drain 后按 topic 分组发送，同一 topic 内顺序由 MQ 的 partition key 保证。

**Q: 为什么不需要线程池了？**

旧版用 `ThreadPoolExecutor` + 虚线程包装同步发送模拟异步。现在所有 MQ 客户端直接返回原生异步结果（RocketMQ v5 `send(msg, SendCallback)` → `CompletableFuture`、Pulsar `sendAsync()` → `CompletableFuture`、Kafka `ListenableFuture` → `CompletableFuture`），上层仅需 `BufferedTrigger` 单线程 drain + `CompletableFuture` 回调，不再需要额外线程池。

**Q: 异步发送如何限流？**

`BufferedTrigger` 内存队列容量 `publisher.batch.queue-capacity`（默认 65536）即天然背压。队列满时 `LinkedBlockingQueue.offer` 返回 false，调用方直接收到 `RejectedExecutionException`，等同于限流拒绝。

---

## 13. 模块结构

```
architect-event/
├── architect-event-commons/          # 公共接口和 POJO
│   └── core/
│       ├── publish/                  # EventPublisher、BatchMessagePublisher、BatchEventMarker
│       └── subscribe/                # SubscribeHandler、SubscribeEventMetadata
├── architect-event-boot-starter/     # 本模块 — Spring Boot 自动配置
│   └── src/main/java/com/cloud/arch/event/
│       ├── boot/                     # 自动配置类 (CloudEventAutoConfiguration)
│       ├── commons/                  # ApplicationContextHolder
│       ├── props/                    # 配置属性 (EventProperties)
│       ├── publisher/                # EventPublisherSynchronization、DomainEventPublisher
│       ├── subscribe/                # 订阅处理器、幂等检查器
│       │   └── impl/                 # JDBC 幂等实现、事务型幂等实现
│       ├── expression/               # SpEL 表达式缓存
│       └── extension/
│           ├── queue/                # Kafka/Pulsar/Rabbit/RocketMQ 扩展配置
│           └── storage/              # JDBC/RocksDB 存储扩展配置
├── architect-event-storage/          # 事件持久化层（JDBC/RocksDB 实现）
│   └── architect-event-storage-jdbc/
│       └── JdbcCompensateProcessor   # 批量补偿（按 topic 分组 + publishBatch + batchCompensate）
├── architect-event-queue/            # 消息队列发布/订阅实现
└── architect-event-watcher/          # HTTP 补偿通道（RocksDB 方案用）
```

---

## 14. 类似组件使用场景对比

### 14.1 功能对比

| 特性 | architect-event | Spring ApplicationEvent | Guava EventBus | Eventuate Tram | Axon Framework |
|:---|:---:|:---:|:---:|:---:|:---:|
| **跨进程投递** | ✅ | ❌ | ❌ | ✅ | ✅ |
| **事务一致性** | ✅ TransactionSynchronization | ✅ @TransactionalEventListener | ❌ | ✅ CDC 捕获 | ✅ UnitOfWork |
| **幂等保障** | ✅ INSERT IGNORE 原子去重 | ❌ | ❌ | ✅ 消息表去重 | ✅ 聚合标识去重 |
| **失败补偿** | ✅ 定时扫描 + 重试 | ❌ | ❌ | ✅ | ✅ Saga 编排 |
| **消息持久化** | ✅ JDBC / RocksDB | ❌ | ❌ | ✅ 事件表 | ✅ 事件存储 |
| **消息队列支持** | 6 种 (Kafka/RocketMQ/Pulsar/Rabbit/ONS) | 无 | 无 | Kafka + JDBC | Kafka/RabbitMQ/JDBC |
| **代码侵入性** | 低，`publish(event)` 静态调用 | 低，注入 ApplicationEventPublisher | 低，注入 EventBus | 高，需聚合根模型 | 高，需 CQRS 范式 |
| **DDD 绑定** | 否，通用设计 | 否 | 否 | 是，聚合根 + 命令 | 是，聚合根 + 命令总线 |
| **Event Sourcing** | ❌ | ❌ | ❌ | ❌ | ✅ |
| **分库分表支持** | ✅ SpEL 分片键 | ❌ | ❌ | ❌ | ❌ |
| **幂等记录清理** | ✅ 分布式锁 + 定时调度 | ❌ | ❌ | ❌ | ❌ |
| **框架体积** | 轻量（~20 类） | 内置 Spring | 轻量（~10 类） | 重量（数十模块） | 重量（数十模块） |
| **上手时间** | < 30 分钟 | < 10 分钟 | < 10 分钟 | 数天 | 数天 |
| **Spring Boot 集成** | ✅ 自动配置 | ✅ 原生内置 | ❌ 需手动 | ✅ | ✅ |

### 14.2 场景选型

| 场景 | 推荐方案 | 理由 |
|------|:---:|------|
| 单体应用内组件解耦 | Spring ApplicationEvent | 零成本，Spring 原生能力 |
| 轻量异步通知（日志、埋点） | Guava EventBus | 无外部依赖，即插即用 |
| **跨服务业务事件 + 幂等 + 补偿** | **architect-event** | 事务同步 + 原子幂等 + 定时补偿完整闭环 |
| DDD + CQRS + Event Sourcing | Axon Framework | 完整范式支持，聚合根建模 |
| 基于数据库 CDC 无侵入投递 | Eventuate Tram | Binlog 捕获，业务代码零感知 |

### 14.3 典型场景代码对比

**Spring ApplicationEvent** — 单体应用内通知：

```java
// 发布
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    eventPublisher.publishEvent(new OrderCreatedEvent(order));
}

// 订阅
@TransactionalEventListener
public void onOrderCreated(OrderCreatedEvent event) {
    notificationService.send(event);  // 同一事务的后置操作
}
```

> 局限：事件无法跨 JVM；若 afterCommit 回调失败，无补偿机制。

**architect-event** — 跨服务可靠投递：

```java
// 发布端
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    DomainEventPublisher.publish(new OrderCreatedEvent(order));
    // 自动：beforeCommit 持久化 → afterCommit 投递 MQ → 失败自动补偿
}

// 消费端
@EventListener
public void onOrderCreated(OrderCreatedEvent event) {
    // 自动：INSERT IGNORE 幂等 → 处理完成 COMMIT
    notificationService.send(event);
}
```

> 优势：事务同步 + 幂等保障 + 失败补偿完整闭环，跨服务可用。

**Guava EventBus** — 轻量异步解耦：

```java
// 发布
eventBus.post(new OrderCreatedEvent(order));

// 订阅
@Subscribe
public void onOrderCreated(OrderCreatedEvent event) {
    notificationService.send(event);
}
```

> 局限：无事务概念，异常吞掉后续 subscriber；进程重启事件丢失。

**Axon Framework** — DDD + CQRS：

```java
// 命令端
@CommandHandler
public void handle(CreateOrderCommand cmd) {
    Aggregate<Order> order = orderFactory.create();
    order.invoke(o -> o.create(cmd));
}

// 事件端
@EventHandler
public void on(OrderCreatedEvent event) {
    orderViewRepository.save(new OrderView(event));
}
```

> 优势：完整 CQRS 栈。代价：强制聚合根建模，框架侵入性高，学习曲线陡峭。

**Eventuate Tram** — CDC 无侵入：

```java
// 业务代码仅需操作聚合根，无需手动发布事件
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    // 框架自动捕获 Order 的 Domain Events，通过 CDC 投递到 Kafka
}
```

> 优势：业务代码零事件感知。代价：依赖 MySQL binlog / Postgres WAL，运维复杂度高。

### 14.4 选型决策树

```
需要跨服务投递？
├── 否 → 需要事务后置处理？
│        ├── 是 → Spring ApplicationEvent
│        └── 否 → Guava EventBus
└── 是 → 需要 DDD + CQRS + Event Sourcing？
         ├── 是 → Axon Framework
         └── 否 → 依赖数据库 CDC？
                  ├── 是 → Eventuate Tram
                  └── 否 → architect-event  ← 大多数场景在此
```

---

## 15. 架构升级记录：sync 线程池 → async 批量原生

### 15.1 架构模型对比

| 维度 | 旧版（v1.0） | 新版（v2.0） |
|------|-------------|-------------|
| 发送模型 | 同步阻塞 `send()` 等待响应 | 原生异步 `sendAsync()` / `send(callback)` → `CompletableFuture<Void>` |
| 线程模型 | `ThreadPoolExecutor`（core=2, max=8） + `runAsync` 包装 | 零额外线程，MQ 客户端 IO 线程直接回调 |
| 攒批入口 | `MessageQueuePublisher` 逐条 `submit` 到线程池 | `BatchMessagePublisher` + `BufferedTrigger` 非阻塞入队攒批 drain |
| 批量发送 | 逐条循环 `publish(msg)` | RocketMQ v5 `producer.send(Collection, callback)` 单次网络往返 |
| 状态标记 | 每条 `markSucceeded` → 1 次 DB UPDATE | `BatchEventMarker` + `BufferedTrigger` → `batchUpdate` 合并 |
| 补偿审计 | 逐条 `compensate()` INSERT | `batchCompensate()` → `jdbcTemplate.batchUpdate` |
| 限流方式 | 线程池队列 8K + `RejectedExecutionException` | 内存队列 64K（默认）+ `offer` 失败拒绝 |

### 15.2 调用链路对比

```
旧版（v1.0）：
afterCommit
  └→ MessageQueuePublisher.publish(entities)
       └→ entities.forEach(e → executor.submit(() → doPublish(e)))
            └→ eventPublisher.publish(msg)          ← 同步阻塞等待 MQ 响应
            └→ markSucceeded/markFailed(entity)     ← 每条一次 UPDATE

新版（v2.0）：
afterCommit
  └→ BatchMessagePublisher.publish(entities)
       └→ BufferedTrigger.offer(entities)            ← 非阻塞入队，O(1)
       └→ SleepyTask drain（batchSize + timeout）
            └→ groupBy topic
                 └→ eventPublisher.publishBatch(msgs) ← 原生异步，单次网络往返
                      └→ CompletableFuture.whenComplete
                           └→ BatchEventMarker.markSucceeded/markFailed ← 攒批批量 UPDATE
```

### 15.3 关键性能提升

**线程消除**

旧版每条消息占用一个线程等待 MQ 响应（~100ms IO wait），500 TPS 需 ~50 个线程常驻。新版 MQ 客户端 IO 线程直接回调 `CompletableFuture`，零业务线程等待。

**网络往返合并（RocketMQ v5）**

旧版 100 条消息 = 100 次 `send()` 网络往返。新版 `producer.send(Collection)` = **1 次网络往返**，吞吐量提升约 50~100 倍。

**DB 写入合并**

| 操作 | 旧版 | 新版 |
|------|------|------|
| 状态标记 | N 条 × 1 UPDATE | N 条 → `batchUpdate`（marker.interval 内攒批） |
| 补偿审计 | N 条 × 1 INSERT | N 条 → `batchUpdate`（allOf 完成后一次写入） |

以 200 条/批次估算，DB 交互从 400 次降至 2 次，**减少约 99%**。

**端到端吞吐量估算**

```
旧版（200 条事件）:
  DB: 200 INSERT + 200 UPDATE + 200 INSERT(审计) = 600 次
  MQ: 200 次 send() 同步等待 ≈ 200 × 100ms = 20s

新版（200 条事件）:
  DB: 1 batchInsert + 2 batchUpdate + 1 batchInsert(审计) = 4 次
  MQ: 1 次 send(Collection) ≈ 100ms
```

端到端延迟从 **秒级降至毫秒级**，吞吐量提升 **50~200 倍**（取决于 MQ 类型和批大小）。

### 15.4 配置迁移

| 旧版配置（已废弃） | 新版替代 |
|-------------------|---------|
| `com.cloud.event.publisher.publish-threads` | — 不再需要 |
| `com.cloud.event.publisher.max-publish-threads` | — 不再需要 |
| `com.cloud.event.publisher.publish-cached-event-size` | `publisher.batch.queue-capacity` |
| `com.cloud.event.publisher.jdbc.compenstate-cron` | `publisher.jdbc.initial-delay` + `period` |

> 注意：`PublishEventProperties` 已重命名为 `EventProperties`。

---

## 16. 版本兼容

| 组件 | 依赖 | 说明 |
|------|------|------|
| Spring Boot | 3.x+ | `AutoConfiguration.imports` 机制 |
| JDK | 17+ | 使用 `instanceof` 模式匹配 |
| Redisson | 4.x+ | 分布式锁（通过 architect-mutex） |
| 数据库 | MySQL 5.7+ / MariaDB 10.3+ | `INSERT IGNORE` 语法 |
