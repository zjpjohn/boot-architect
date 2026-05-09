# architect-transaction 异步事务日志组件

## 1. 概述

architect-transaction 是一个基于**本地事务表 + 最终一致性**模式的轻量级异步任务组件。通过 `@TxAsync` 注解将方法调用与当前事务绑定，事务提交后异步执行，失败自动指数退避重试，确保异步任务**不丢失、不重复执行**。

### 核心特性

- **零代码侵入** — 一个 `@TxAsync` 注解即可接入
- **事务绑定** — 任务落库与业务操作在同一本地事务中，保证 at-least-once
- **四层防护** — 即时执行 → 延迟重试 → 补偿扫描 → 僵死修复
- **指数退避** — 重试间隔 30s → 60s → 120s → ... → 最大 8 次
- **死信机制** — 超过最大重试次数自动标记 DEAD，停止重试
- **版本控制** — 支持 API 版本号，防止代码升级导致重试失败
- **分库分表** — 原生 shardKey 支持
- **分布式锁调度** — 多实例部署时只有一个实例执行补偿任务

### 与 architect-event 的关系

| 组件 | 定位 | 投递方式 | 适用场景 |
|------|------|---------|---------|
| architect-transaction | 单服务内异步任务 | 线程池直接调用 | 发短信、推送、异步计算 |
| architect-event | 跨服务领域事件 | 消息队列投递 | 订单支付后通知库存、积分 |

---

## 2. 快速开始

### 2.1 引入依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-transaction-async-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2.2 创建任务表

```sql
CREATE TABLE arch_tx_log (
    id              BIGINT        NOT NULL COMMENT '任务ID',
    async_key       VARCHAR(128)  NOT NULL COMMENT '任务标识(类名.方法名)',
    shard_key       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '分片键',
    data            TEXT          NOT NULL COMMENT '任务参数(JSON)',
    version         VARCHAR(32)   NOT NULL DEFAULT '1.0' COMMENT '任务版本',
    state           TINYINT       NOT NULL COMMENT '状态:1-READY,2-RUNNING,3-SUCCESS,4-FAIL,5-DEAD',
    max_retry       INT           NOT NULL DEFAULT 8 COMMENT '最大重试次数',
    retry_interval  BIGINT        NOT NULL DEFAULT 30 COMMENT '重试间隔基数(秒)',
    retries         INT           NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    next_time       DATETIME      NULL COMMENT '下次重试时间',
    gmt_create      DATETIME      NOT NULL COMMENT '创建时间',
    gmt_modify      DATETIME      NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id, shard_key),
    INDEX idx_state_next_time (state, next_time),
    INDEX idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步事务日志表';
```

### 2.3 配置

```yaml
com.cloud.async.transaction:
  batch: 50                       # 补偿批量大小
  initial-delay: 10s              # 补偿启动延迟
  period: 60s                     # 补偿扫描间隔
  mutex:
    initial-delay: 5s             # 分布式锁初始延迟
    ttl: 30s                      # 锁过期时间
    transition: 15s               # 锁续期时间
  business:                       # 业务执行线程池
    core: 2
    max-size: 32
    keep-alive: 600
    queue-size: 4096
  retry:                          # 重试执行线程池
    core: 1
    max-size: 4
    keep-alive: 300
    queue-size: 1024
```

### 2.4 编写异步任务

```java
@Service
public class SmsService {

    @TxAsync(name = "sendSms", retryInterval = 30L, maxRetry = 5)
    public void sendSms(String phone, String content) {
        // 业务逻辑：调用短信网关
        smsGateway.send(phone, content);
    }
}
```

### 2.5 调用

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder(OrderDTO dto) {
        // 业务操作
        orderRepository.save(order);
        // 调用 @TxAsync 方法 → 事务提交后异步执行
        smsService.sendSms(dto.getPhone(), "订单创建成功");
    }
}
```

---

## 3. 核心概念

### 3.1 任务状态机

```
                    ┌─────────┐
                    │  READY  │ ← 事务内落库
                    └────┬────┘
                         │ 线程池执行
                    ┌────▼────┐
                    │ RUNNING │
                    └────┬────┘
                    ┌────┴────┐
                    │         │
               ┌────▼───┐ ┌──▼────┐
               │SUCCESS │ │ FAIL  │ → calcNextTime() → 进重试队列
               └────────┘ └──┬────┘
                             │ retries >= maxRetry
                        ┌────▼───┐
                        │  DEAD  │ → 停止重试，人工介入
                        └────────┘
```

### 3.2 任务生命周期

```
事务内 beforeCommit          事务外 afterCommit           定时补偿                 僵死修复
    │                              │                       │                      │
    ▼                              ▼                       ▼                      ▼
落库(READY) ─────────────────→ 线程池执行 ──→ 成功(SUCCESS)  扫描FAIL任务 ──→ 重试  扫描READY/RUNNING
                                    │                   │                      │
                                    └──→ 失败(FAIL) ────┘                      └──→ 重新执行
                                          │
                                          └──→ 超最大重试 → DEAD
```

---

## 4. @TxAsync 注解

### 4.1 参数说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | `""` | 任务名称。同方法名出现多次时用于区分，拼接到 asyncKey 中 |
| `version` | String | `"1.0"` | 任务版本。方法参数变更时升级版本，旧版本事件会自动跳过 |
| `retryInterval` | long | `30L` | 重试间隔基数(秒)。指数增长：30, 60, 120, 240, ... |
| `maxRetry` | int | `8` | 最大重试次数。超过后标记 DEAD |

### 4.2 使用约束

- 方法返回值必须为 `void`
- 必须在 Spring 事务上下文中调用（`@Transactional` 方法内）
- 方法参数必须能被 fastjson2 序列化/反序列化
- `retryInterval` 不能小于 10 秒

### 4.3 asyncKey 生成规则

```
asyncKey = 类名 + "." + 方法名 + "." + name

示例:
  SmsService.sendSms.sendSms          (name = "sendSms")
  SmsService.batchSend.v2             (name = "v2")
```

同一类中同名方法需通过 `name` 区分，否则启动校验报错。

---

## 5. 配置参考

### 5.1 完整配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `com.cloud.async.transaction.batch` | Integer | 50 | 每次补偿扫描拉取的最大失败任务数 |
| `com.cloud.async.transaction.initial-delay` | Duration | 10s | 补偿任务首次启动延迟 |
| `com.cloud.async.transaction.period` | Duration | 60s | 补偿任务扫描间隔 |
| `com.cloud.async.transaction.mutex.initial-delay` | Duration | 5s | 分布式锁初始延迟 |
| `com.cloud.async.transaction.mutex.ttl` | Duration | 30s | 锁过期时间 |
| `com.cloud.async.transaction.mutex.transition` | Duration | 15s | 锁续期时间 |
| `com.cloud.async.transaction.business.core` | Integer | 2 | 业务执行线程池核心线程数 |
| `com.cloud.async.transaction.business.max-size` | Integer | 32 | 业务执行线程池最大线程数 |
| `com.cloud.async.transaction.business.keep-alive` | Integer | 600 | 线程活跃时间(秒) |
| `com.cloud.async.transaction.business.queue-size` | Integer | 4096 | 缓冲队列大小 |
| `com.cloud.async.transaction.retry.core` | Integer | 1 | 重试线程池核心线程数 |
| `com.cloud.async.transaction.retry.max-size` | Integer | 4 | 重试线程池最大线程数 |
| `com.cloud.async.transaction.retry.keep-alive` | Integer | 300 | 线程活跃时间(秒) |
| `com.cloud.async.transaction.retry.queue-size` | Integer | 1024 | 缓冲队列大小 |

### 5.2 线程隔离

业务执行和重试执行使用**独立线程池**，避免重试任务占满业务线程：

```
business-executor (core=2, max=32, queue=4096)
    ├─ 处理事务提交后的首次执行
    └─ 处理僵死修复任务

retry-executor (core=1, max=4, queue=1024)
    └─ 处理补偿重试任务
```

### 5.3 拒绝策略

两个线程池均使用 `DiscardPolicy`，队列满时静默丢弃。生产环境应确保 `queue-size` 足够大或升级为 `CallerRunsPolicy`。

---

## 6. 架构流程

### 6.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Spring 事务边界                           │
│  ┌──────────────┐     ┌──────────────────┐                      │
│  │ 业务代码      │ ──→ │ @TxAsync 方法    │                      │
│  │ order.save() │     │ sms.send(phone)  │                      │
│  └──────────────┘     └────────┬─────────┘                      │
│                                │ AOP 拦截                       │
│                                ▼                                │
│                       ┌─────────────────┐                       │
│                       │ AsyncTxEventHolder│ ← ThreadLocal 暂存  │
│                       └────────┬────────┘                       │
│                                │                                │
│  ┌─────────────────────────────▼──────────────────────────────┐ │
│  │              TransactionSynchronization                     │ │
│  │  beforeCommit → JdbcAsyncTxRepository.initialize()  落库   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼ afterCommit
                    ┌─────────────────────┐
                    │  AsyncTxExecutor     │
                    │  (business-executor) │
                    └────────┬────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                成功(SUCCESS)    失败(FAIL)
                                    │
                                    ▼
                          calcNextTime() 计算下次重试时间
                                    │
                                    ▼
                    ┌────────────────────────────┐
                    │  AsyncCompensateScheduler   │ ← 定时扫描 FAIL
                    │  (分布式锁，单实例执行)       │
                    └────────────┬───────────────┘
                                 │
                                 ▼
                    ┌────────────────────────────┐
                    │     AsyncRetryQueue         │
                    │  DelayQueue 指数退避重试     │
                    │  (retry-executor 执行)      │
                    └────────────┬───────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                成功(SUCCESS)           失败(FAIL) → 再次进队列
                                           │
                                      retries >= maxRetry
                                           │
                                           ▼
                                         DEAD
```

### 6.2 核心组件职责

| 组件 | 职责 | 生命周期 |
|------|------|---------|
| `AsyncTransactionInterceptor` | AOP 拦截 `@TxAsync`，构建事件放入 ThreadLocal | 单例 |
| `AsyncTxEventHolder` | 线程级暂存异步事件 + 注册事务同步器 | ThreadLocal |
| `AsyncTxSynchronization` | beforeCommit 落库 / afterCommit 执行 / afterCompletion 清理 | 事务级 |
| `AsyncTxExecutor` | 业务线程池执行异步任务 | 单例 |
| `AsyncCompensateScheduler` | 定时扫描 FAIL 任务，放入重试队列 | 单例 |
| `AsyncRetryQueue` | DelayQueue 管理重试延迟 | 单例（内含守护线程） |
| `AsyncReparationScheduler` | 修复异常关闭导致的 READY/RUNNING 僵死任务 | 单例 |
| `AsyncTxInvokerProcessor` | 启动时扫描所有 `@TxAsync` Bean，构建 Invoker 注册表 | 单例 |

### 6.3 SQL 说明

| SQL | 说明 |
|-----|------|
| `INIT_SQL` | beforeCommit 批量插入，与业务同事务 |
| `MARK_RUNNING_SQL` | 执行前标记 RUNNING，含 shard_key 条件 |
| `MARK_SUCCESS_SQL` | 执行成功标记 SUCCESS |
| `MARK_FAIL_SQL` | 执行失败标记 FAIL + 更新 retries + next_time |
| `MARK_DEAD_SQL` | 超最大重试标记 DEAD |
| `QUERY_FAIL_SQL` | 补偿扫描：state=4 且 retries < max_retry 且 next_time < now |
| `READY_RUNNING_SQL` | 修复扫描：state in (1,2) 且 gmt_create < 阈值 |

---

## 7. 重试机制

### 7.1 退避策略

```
重试间隔 = retryInterval × 2^retries

retries=0: 30s   (首次重试)
retries=1: 60s
retries=2: 120s
retries=3: 240s
retries=4: 480s
retries=5: 960s  (16分钟)
retries=6: 1920s (32分钟)
retries=7: 3840s (64分钟)
retries=8: 超 maxRetry → DEAD
```

### 7.2 重试队列

使用 `DelayQueue<AsyncRetryTask>` 实现，任务按 `nextTime` 排序：

- 补偿调度器批量拉取 FAIL 任务 → 过滤已存在的 → 放入 DelayQueue
- 守护线程轮询到期任务 → 投递到 retry-executor 执行
- 执行失败 → `markFail()` 重新计算 nextTime → 下轮补偿扫描再次入队

### 7.3 幂等保证

UPDATE SQL 均带 `id + shard_key` 条件，同一任务重复执行不会产生脏数据。任务级别的去重由 `retryEvents` (ConcurrentHashSet) 保证，同一事件不会同时存在于重试队列中。

---

## 8. 版本控制

### 8.1 使用场景

方法参数变更（如新增必填字段）后，DB 中旧版本事件反序列化可能失败。升级 `version` 后，重试时会自动跳过旧版本事件。

```java
// v1.0: 两个参数
@TxAsync(version = "1.0")
public void notify(String userId, String content) { ... }

// v1.1: 新增渠道参数
@TxAsync(version = "1.1")
public void notify(String userId, String content, String channel) { ... }
```

### 8.2 版本比较规则

```
重试时: event.version >= invoker.version → 执行
        event.version <  invoker.version → 跳过 + warn 日志
```

版本格式：`x.x` 或 `x.x.x`，按数字段逐位比较。

---

## 9. 分布式锁调度

补偿任务使用 `architect-mutex-lock` 模块实现分布式锁，保证多实例部署时只有一个实例执行补偿扫描：

```java
mutexTemplate.scheduleAtRate(mutexProps, ASYNC_COMPENSATE_MUTEX, ...);
```

锁参数可配置（见配置参考），默认 TTL 30s，续期 15s，防止执行超时导致锁释放。

---

## 10. 分库分表支持

通过 `AsyncTxSharding` 设置分片键，所有 SQL 操作自动携带 `shard_key`：

```java
// 在事务开始前设置分片键
AsyncTxSharding.shardingKey("merchant_123");

@Transactional
public void processOrder() {
    asyncService.sendNotify(...);  // 自动携带 shard_key
}
```

`afterCompletion` 中自动清理 `shardingContext`，防止线程池复用污染。

---

## 11. 扩展点

### 11.1 自定义存储

实现 `IAsyncTxRepository` 接口，替换 JDBC 为其他存储：

```java
public interface IAsyncTxRepository {
    void initialize(List<AsyncTxEvent> events);
    List<AsyncTxEvent> loadReadyRunning(Duration before);
    void markSuccess(AsyncTxEvent event);
    void markFail(AsyncTxEvent event);
    void markRunning(AsyncTxEvent event);
    List<AsyncTxEvent> queryFailed(int limit, Duration range);
}
```

### 11.2 自定义编解码

实现 `AsyncEventCodec` 接口，替换 fastjson2 为其他序列化方案：

```java
public interface AsyncEventCodec {
    String encode(AsyncTxParams params);
    AsyncTxParams decode(String data);
}
```

---

## 12. 最佳实践

### 12.1 方法设计

```java
// ✅ 推荐：参数简单明确，便于序列化
@TxAsync
public void sendSms(String phone, String templateCode, String content) { ... }

// ❌ 避免：参数含不可序列化对象（HttpRequest、OutputStream 等）
@TxAsync
public void sendSms(HttpServletRequest request) { ... }
```

### 12.2 幂等设计

异步方法本身应具备幂等性，防止极端情况下重复执行：

```java
@TxAsync
public void grantCoupon(String userId, String couponId) {
    if (couponRepository.exists(userId, couponId)) {
        return;  // 已发放，幂等跳过
    }
    couponRepository.grant(userId, couponId);
}
```

### 12.3 超时控制

确保异步方法有合理的超时机制，防止长时间占用线程：

```java
@TxAsync(retryInterval = 60L)
public void callExternalApi(String data) {
    // 配置 RestTemplate/HttpClient 超时，而非无限等待
    externalApi.call(data);
}
```

### 12.4 死信处理

定期监控 DEAD 状态任务，提供人工重跑或丢弃机制：

```sql
-- 查询死信任务
SELECT * FROM arch_tx_log WHERE state = 5;

-- 手动重置重试
UPDATE arch_tx_log SET state = 4, retries = 0, next_time = NOW()
WHERE id = ? AND shard_key = ?;
```

### 12.5 线程池配置

| 场景 | business.core | business.max-size | retry.core | retry.max-size |
|------|:--:|:--:|:--:|:--:|
| 低并发 (< 100 TPS) | 2 | 8 | 1 | 2 |
| 中并发 (100-500 TPS) | 4 | 16 | 2 | 4 |
| 高并发 (> 500 TPS) | 8 | 64 | 4 | 8 |

---

## 13. FAQ

**Q1: 事务回滚了，任务还会执行吗？**

不会。`beforeCommit` 在事务内执行，事务回滚则落库失败。`afterCommit` 只在事务成功提交后触发。

**Q2: 服务重启后，正在执行的任务会丢失吗？**

不会。`AsyncReparationScheduler` 会扫描状态为 READY/RUNNING 且创建时间早于阈值的任务，重新投递执行。

**Q3: 如何确认任务执行失败的原因？**

查看应用日志，搜索 `asyncKey` 或任务 ID。每次失败都会打印 `log.error(error.getMessage(), error)`。

**Q4: 可以不使用分布式锁吗？**

当前版本强依赖 `architect-mutex-lock`。如需独立使用，可替换 `AsyncCompensateScheduler` 和 `AsyncReparationScheduler` 的实现。

**Q5: 与 @Async 的区别？**

| | @TxAsync | @Async |
|---|:--:|:--:|
| 事务绑定 | ✅ 事务提交后执行 | ❌ 立即执行，独立事务 |
| 持久化 | ✅ 落库 | ❌ 内存 |
| 重试 | ✅ 自动退避 | ❌ 需手写 |
| 适用 | 核心异步任务 | 非关键旁路逻辑 |

---

## 14. 模块结构

```
architect-transaction/
├── architect-transaction-async-support/     # 核心抽象层
│   └── src/main/java/com/cloud/arch/transaction/
│       ├── annotation/TxAsync.java          # @TxAsync 注解
│       ├── codec/
│       │   ├── AsyncEventCodec.java         # 编解码接口
│       │   └── JsonEventCodec.java          # fastjson2 实现
│       ├── core/
│       │   ├── AsyncTxEvent.java            # 任务事件实体
│       │   ├── AsyncTxExecutor.java         # 任务执行器
│       │   ├── AsyncTxInvoker.java          # 方法调用器
│       │   ├── AsyncTxInvokers.java         # 调用器注册表
│       │   ├── AsyncTxParams.java           # 任务参数
│       │   ├── AsyncTxSharding.java         # 分片键上下文
│       │   ├── AsyncTxVersion.java          # 版本号
│       │   └── IAsyncTxRepository.java      # 仓储接口
│       ├── support/
│       │   ├── AsyncRetryQueue.java         # 延迟重试队列
│       │   └── AsyncRetryTask.java          # 重试任务包装
│       └── utils/AsyncTxState.java          # 状态常量
│
└── architect-transaction-async-starter/     # 自动配置层
    └── src/main/java/com/cloud/arch/transaction/
        ├── boot/AsyncTxAutoConfiguration.java      # 自动配置
        ├── config/AsyncTaskProperties.java         # 配置属性
        ├── interceptor/AsyncTransactionInterceptor.java  # AOP 拦截器
        └── support/
            ├── ApplicationContextHolder.java       # 容器持有者
            ├── AsyncCompensateScheduler.java       # 补偿调度器
            ├── AsyncReparationScheduler.java       # 修复调度器
            ├── AsyncTxEventHolder.java             # 事件暂存器
            ├── AsyncTxInvokerProcessor.java        # Invoker 扫描注册
            ├── AsyncTxSynchronization.java         # 事务同步器
            └── JdbcAsyncTxRepository.java          # JDBC 仓储实现
```
