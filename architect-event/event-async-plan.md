# EventPublisher 异步化方案：void → CompletableFuture

## 背景

所有 5 个 MQ 客户端的 `publish()` 实现都是同步阻塞的，异步能力由上层 `MessageQueuePublisher` / `BatchMessagePublisher` 用 `CompletableFuture.runAsync` + 虚线程模拟。但实际上每个 MQ 客户端都支持原生异步 API：

| MQ 实现 | 当前 sync 调用 | 原生 async 能力 |
|---------|---------------|----------------|
| RocketMQ v5 | `producer.send(msg, timeout)` | `producer.send(msg, SendCallback)` |
| Pulsar 3.3 | `builder.send()` | `builder.sendAsync()` → `CompletableFuture<MessageId>` |
| ONS | `producer.send(msg)` | `producer.sendAsync(msg, SendCallback)` |
| Kafka | `template.send().get(10s)` → 人为阻塞 | `template.send()` 返回 `ListenableFuture`，本就异步 |
| RabbitMQ | `template.send(routing, msg)` | 无原生 async，需 `CompletableFuture.runAsync` 适配 |

## 目标

将 `EventPublisher` 接口改为返回 `CompletableFuture<Void>`，消除上层 `asyncExecutor` 线程池，让 MQ 客户端原生异步能力直达底层。

## 改动范围

### 文件 1：`EventPublisher.java` — 接口签名变更

**路径：** `architect-event/architect-event-commons/src/main/java/com/cloud/arch/event/core/publish/EventPublisher.java`

```java
public interface EventPublisher {
    CompletableFuture<Void> publish(EventMessage message);

    default CompletableFuture<Void> publishBatch(List<EventMessage> messages) {
        return CompletableFuture.allOf(
            messages.stream().map(this::publish).toArray(CompletableFuture[]::new)
        );
    }
}
```

### 文件 2-6：5 个 MQ 实现适配

#### RocketEventPublisher (RocketMQ v5)
- 路径：`architect-event-queue/architect-event-queue-rocketmq-v5x/.../RocketEventPublisher.java`
- 改动：`producer.send()` → `CompletableFuture` 包装 `SendCallback`
```java
public CompletableFuture<Void> publish(EventMessage message) {
    Message msg = checkAndConvert(message);
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
        producer.send(msg, new SendCallback() {
            public void onSuccess(SendResult result) { future.complete(null); }
            public void onException(Throwable e) { future.completeExceptionally(e); }
        });
    } catch (Exception e) {
        future.completeExceptionally(new RuntimeException(e));
    }
    return future;
}
```

#### PulsarEventPublisher (Pulsar 3.3)
- 路径：`architect-event-queue/architect-event-queue-pulsar/.../PulsarEventPublisher.java`
- 改动：`builder.send()` → `builder.sendAsync().thenApply(id -> null)`，最简洁
```java
public CompletableFuture<Void> publish(EventMessage message) {
    // ... 校验逻辑不变
    return producer.newMessage()...build().sendAsync().thenApply(id -> null);
}
```

#### OnsEventPublisher (ONS)
- 路径：`architect-event-queue/architect-event-queue-rocketmq-ons/.../OnsEventPublisher.java`
- 改动：同 RocketMQ 模式，`producer.sendAsync()` + `CompletableFuture`
```java
public CompletableFuture<Void> publish(EventMessage message) {
    Message msg = checkAnConvert(message);
    CompletableFuture<Void> future = new CompletableFuture<>();
    producer.sendAsync(msg, new SendCallback() {
        public void onSuccess(SendResult result) { future.complete(null); }
        public void onException(OnExceptionContext ctx) {
            future.completeExceptionally(ctx.getException());
        }
    });
    return future;
}
```

#### KafkaEventPublisher
- 路径：`architect-event-queue/architect-event-queue-kafka/.../KafkaEventPublisher.java`
- 改动：删除 `.get(10, TimeUnit.SECONDS)`，直接返回 future（Spring Kafka 3.x 的 `KafkaTemplate.send()` 返回 `CompletableFuture`）
```java
public CompletableFuture<Void> publish(EventMessage message) {
    ProducerRecord<String, String> record = checkAndConvert(message);
    return template.send(record).thenApply(r -> null);
}
```

#### RabbitEventPublisher
- 路径：`architect-event-queue/architect-event-queue-rabbitmq/.../RabbitEventPublisher.java`
- 改动：RabbitMQ 无原生 async send，用 `CompletableFuture.runAsync(虚线程)` 适配；新增 `DisposableBean` 管理线程
```java
public class RabbitEventPublisher implements EventPublisher, DisposableBean {
    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("rabbit-publisher-").factory());

    public CompletableFuture<Void> publish(EventMessage message) {
        return CompletableFuture.runAsync(
            () -> rabbitTemplate.send(routingKey, msg), executor);
    }

    public void destroy() { executor.shutdown(); }
}
```

### 文件 7：`MessageQueuePublisher.java` — 移除 asyncExecutor

**路径：** `architect-event/architect-event-commons/.../MessageQueuePublisher.java`

核心变化：
- 移除 `executorService` 字段、构造函数中的创建、`destroy()` 中的关闭
- `publish(EventMessage)`：直接返回 `eventPublisher.publish()` 的 future，信号量在 `.whenComplete()` 中释放
- `publish(List<PublishEventEntity>)`：异步路径直接返回 `eventPublisher.publishBatch()` 的 future
- `doPublish`：返回 `CompletableFuture<Void>`，stats + 标记在 `.whenComplete()` 回调

```java
// 移除 executorService 字段
private final Semaphore semaphore;
private final BatchEventMarker batchMarker;

public MessageQueuePublisher(Integer maxConcurrency, BatchEventMarker batchMarker) {
    this.semaphore = new Semaphore(maxConcurrency);
    this.batchMarker = batchMarker;
}

private CompletableFuture<Void> publish(EventMessage message) {
    if (!semaphore.tryAcquire()) {
        log.warn("max concurrency reached, fallback to sync publish");
        return eventPublisher.publish(message)
            .exceptionally(ex -> { log.error(ex.getMessage(), ex); return null; });
    }
    return eventPublisher.publish(message)
        .whenComplete((v, ex) -> {
            semaphore.release();
            if (ex != null) log.error(ex.getMessage(), ex);
        });
}

// publish(List) 异步路径简化：
return eventPublisher.publishBatch(messages)
    .whenComplete((v, ex) -> semaphore.release());

// doPublish 改为返回 CompletableFuture：
private CompletableFuture<Void> doPublish(PublishEventEntity entity) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    return eventPublisher.publish(entity.build())
        .whenComplete((v, ex) -> {
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (ex != null) {
                statsManager.statsCounter(entity.getName()).recordPublishFailure(elapsed);
                batchMarker.markFailed(entity);
            } else {
                statsManager.statsCounter(entity.getName()).recordPublishSuccess(elapsed);
                batchMarker.markSucceeded(entity);
            }
        });
}

// publish(List) fallback 路径改为收集 CompletableFuture：
if (!semaphore.tryAcquire()) {
    log.warn("max concurrency reached, fallback to sync publish for {} events", batch.size());
    CompletableFuture<?>[] futures = batch.stream().map(entity -> {
        statsManager.statsCounter(entity.getName()).recordPublishFallbackSync();
        return doPublish(entity);
    }).toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures);
}

// destroy(): 移除 executorService 关闭逻辑，只保留空实现或移除方法
```

### 文件 8：`BatchMessagePublisher.java` — 移除 asyncExecutor

**路径：** `architect-event/architect-event-commons/.../BatchMessagePublisher.java`

核心变化：
- 移除 `asyncExecutor` 字段、构造函数中的创建、`destroy()` 中的关闭
- `processBatch` 异步路径：`eventPublisher.publishBatch()` 返回 CompletableFuture，stats + 标记在 `.whenComplete()` 回调
- `doPublish`：同 MessageQueuePublisher，返回 `CompletableFuture<Void>`
- `destroy()`：只保留 `trigger.shutdown()` + `drainExecutor.shutdown()`

```java
// 构造函数：移除 asyncExecutor
public BatchMessagePublisher(...) {
    this.semaphore = new Semaphore(maxConcurrency);
    this.batchMarker = batchMarker;
    this.drainExecutor = Executors.newSingleThreadExecutor(Threads.threadFactory("outbox-stealer"));
    // ... BufferedTrigger 构建不变
}

// processBatch 异步路径：
List<EventMessage> messages = group.stream().map(PublishEventEntity::build).toList();
String topic = group.get(0).getName();
Stopwatch stopwatch = Stopwatch.createStarted();
eventPublisher.publishBatch(messages)
    .whenComplete((v, ex) -> {
        semaphore.release();
        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        if (ex != null) {
            log.error("batch publish error for topic:[{}], count:[{}]", topic, group.size(), ex);
            group.forEach(entity -> {
                statsManager.statsCounter(entity.getName()).recordPublishFailure(elapsed);
                batchMarker.markFailed(entity);
            });
        } else {
            group.forEach(entity -> {
                statsManager.statsCounter(entity.getName()).recordPublishSuccess(elapsed);
                batchMarker.markSucceeded(entity);
            });
        }
    });

// destroy(): 只保留 drainExecutor 和 trigger
public void destroy() {
    trigger.shutdown();
    drainExecutor.shutdown();
    try { if (!drainExecutor.awaitTermination(5, SECONDS)) drainExecutor.shutdownNow(); }
    catch (InterruptedException e) { drainExecutor.shutdownNow(); Thread.currentThread().interrupt(); }
}
```

## 架构对比

```
之前：上层用虚线程模拟异步，底层同步阻塞
  BatchMessagePublisher ──runAsync(虚线程)──▶ eventPublisher.publishBatch() [同步阻塞]
  MessageQueuePublisher ──runAsync(虚线程)──▶ eventPublisher.publish()      [同步阻塞]

之后：底层原生异步，上层直接返回 CompletableFuture
  BatchMessagePublisher ──▶ eventPublisher.publishBatch() ──▶ MQ 原生异步
  MessageQueuePublisher ──▶ eventPublisher.publish()      ──▶ MQ 原生异步
```

消除的资源：
- `MessageQueuePublisher.asyncExecutor`（虚线程池）
- `BatchMessagePublisher.asyncExecutor`（虚线程池）
- `KafkaEventPublisher` 不再 `.get(10s)` 阻塞
- RabbitMQ 适配层自行维护一个虚线程 executor（每个实例一个）

## 不改动的部分

| 组件 | 原因 |
|------|------|
| `EventPublisherSynchronization` | 接口不变，enqueue/publish 调用方不变 |
| `PublishEventProperties` | 配置不变 |
| `CloudEventAutoConfiguration` | Bean 注册逻辑不变 |
| `BatchEventMarker` | 独立组件，不受影响 |
| `JdbcCompensateProcessor` | 补偿路径调 `doPublish`，签名改为返回 CompletableFuture 但行为不变 |

## 验证

1. `mvn compile -pl architect-event/architect-event-commons,architect-event/architect-event-boot-starter,architect-event/architect-event-queue -am`
2. `mvn test -pl architect-event/architect-event-commons`
3. 确认 `EventPublisher` 接口只有 5 个实现（通过 Grep 验证）
4. 各 MQ 模块编译通过（RocketMQ/Pulsar/ONS/Kafka/RabbitMQ）
