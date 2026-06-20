# architect-cache-boot-starter 缓存组件使用文档

## 1. 概述

本组件是 [architect-cache-support](../architect-cache-support/README.md) 二级缓存组件的 Spring Boot 自动配置模块，通过注解驱动 + AOP 拦截的方式为应用提供方法级缓存能力，同时内置热 key 自动探测功能。

### 核心特性

- **零配置启动**：引入依赖即用，所有配置项均有合理默认值
- **注解驱动**：`@CacheResult` / `@CachePut` / `@CacheEvict` 三个注解覆盖 CRUD 缓存场景
- **SpEL 表达式**：支持灵活的 key 生成、条件缓存、排除缓存
- **延迟双删**：基于 `@TransactionalEventListener` + `DelayQueue`，解决数据库主从延迟导致的缓存脏读
- **热 key 探测**：基于 Etcd + Worker 集群的热点自动发现，热点数据自动获得 L1 加速
- **双模式切换**：标准模式 / 热 key 探测模式自动切换，用户无感知
- **多级扩展点**：KeyGenerator / CacheResolver / CacheErrorHandler 等 6 个策略接口可自定义

### 架构简图

```
@CacheResult / @CachePut / @CacheEvict          ← 应用层注解
        ↓
AnnotationCacheAspect (AOP 切面)                 ← 拦截层
        ↓
CacheAspectSupport (执行引擎)                    ← 调度层
  ├── SpEL 条件评估 (condition/unless)
  ├── Key 生成 (KeyGenerator / SpEL)
  ├── Cache 读写 (L1 Caffeine + L2 Redis)
  └── 延迟双删 (DelayQueue + @TransactionalEventListener)
        ↓
CacheManager → RedisCacheManager                 ← 缓存管理层
  ├── RedisRemoteCache (L2)
  └── CaffeineLocalCache (L1, 动态激活)
```

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-cache-boot-starter</artifactId>
</dependency>
```

> 注意：本组件依赖 Redisson，需确保 `RedissonClient` Bean 已自动配置。

### 2.2 最小配置

默认配置下无需任何 `application.yml` 配置即可使用。以下为全量配置参考：

```yaml
com:
  cloud:
    cache:
      refresh-topic: cache:refresh:default     # 集群失效广播 Topic，默认值
      enable-local: true                       # 是否启用 L1 本地缓存，默认 true
      enable-delay-evict: true                 # 是否启用延迟双删，默认 true
      delay-evict-interval: 500                # 延迟删除间隔（毫秒），默认 500
      allow-null-value: true                   # 是否缓存 null 值防穿透，默认 true
      enable-metric: false                     # 是否启用 Micrometer 指标，默认 false
      ttl-refresh-interval: -60                # TTL 刷新间隔（秒），默认 -60
      only-public: true                        # 是否仅代理 public 方法，默认 true
      max-delay-evict-size: 10000              # 延迟删除队列最大容量，默认 10000
    ## 热 key 探测配置（可选，需引入 architect-hotkey 依赖）
    # cache:
    #   hotkey:
    #     etcd-server: http://etcd:2379
    #     app-name: my-app
```

### 2.3 使用注解

```java
@Service
public class UserService {

    @CacheResult(name = "user", key = "#userId")
    public User getUser(Long userId) {
        return userRepository.findById(userId);
    }

    @CachePut(name = "user", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(name = "user", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
```

---

## 3. 注解详解

### 3.1 @CacheResult — 查询缓存

标记方法返回值将被缓存。先查缓存，命中则直接返回；未命中则执行方法并将结果写入缓存。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String[]` | — | 缓存实例名称，必填 |
| `key` | `String` | `""` | 缓存 key 的 SpEL 表达式，与 `keyGenerator` 互斥 |
| `keyGenerator` | `String` | `""` | 自定义 KeyGenerator Bean 名称 |
| `condition` | `String` | `""` | SpEL 条件表达式，满足才缓存 |
| `unless` | `String` | `""` | SpEL 排除表达式，满足则不缓存 |
| `enableLocal` | `boolean` | `false` | 是否启用 L1 本地缓存 |
| `local` | `@Local` | 默认值 | L1 缓存配置 |
| `remote` | `@Remote` | 默认值 | L2 缓存配置 |
| `warmup` | `boolean` | `false` | 是否允许缓存预热，配合 `architect-cache-warmup` 模块使用 |
| `remark` | `String` | `""` | 缓存备注，配合 warmup 使用，在元数据查询中展示业务含义 |

**使用示例**：

```java
// 基础用法
@CacheResult(name = "user", key = "#userId")
public User getUser(Long userId) { ... }

// 条件缓存：仅缓存非空结果
@CacheResult(name = "user", key = "#userId", unless = "#result == null")
public User getUser(Long userId) { ... }

// 开启本地缓存 + 自定义 TTL
@CacheResult(name = "hot:config", key = "#key",
    enableLocal = true,
    local = @Local(maximumSize = 500, expire = 300),
    remote = @Remote(expire = 3600, randomBound = 600))
public Config getConfig(String key) { ... }

// SpEL 表达式引用方法参数属性
@CacheResult(name = "order", key = "#order.id")
public Order getOrder(Order order) { ... }

// 标记为可预热（配合 architect-cache-warmup 模块）
@CacheResult(name = "user_cache", key = "#userId",
             warmup = true, remark = "用户信息缓存，按用户ID预热")
public User getUser(Long userId) { ... }
```

### 3.2 @CachePut — 更新缓存

方法执行后将返回值写入缓存。与 `@CacheResult` 不同，`@CachePut` 始终执行方法体。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String[]` | — | 缓存实例名称，必填 |
| `key` | `String` | `""` | 缓存 key 的 SpEL 表达式 |
| `keyGenerator` | `String` | `""` | 自定义 KeyGenerator Bean 名称 |
| `condition` | `String` | `""` | SpEL 条件表达式 |
| `unless` | `String` | `""` | SpEL 排除表达式 |

**使用示例**：

```java
@CachePut(name = "user", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

### 3.3 @CacheEvict — 删除缓存

删除指定 key 或全部缓存。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String[]` | — | 缓存实例名称，必填 |
| `key` | `String` | `""` | 缓存 key 的 SpEL 表达式 |
| `keyGenerator` | `String` | `""` | 自定义 KeyGenerator Bean 名称 |
| `condition` | `String` | `""` | SpEL 条件表达式 |
| `allEntries` | `boolean` | `false` | 是否清除该缓存实例的全部数据 |
| `beforeInvocation` | `boolean` | `false` | 是否在方法执行前删除缓存（默认执行后） |

**使用示例**：

```java
// 删除单个缓存
@CacheEvict(name = "user", key = "#userId")
public void deleteUser(Long userId) { ... }

// 清除全部缓存
@CacheEvict(name = "user", allEntries = true)
public void clearAllUsers() { ... }

// 方法执行前删除（防止执行期间脏读），不支持延迟双删
@CacheEvict(name = "user", key = "#userId", beforeInvocation = true)
public void evictBeforeUpdate(Long userId) { ... }
```

### 3.4 @Local — L1 缓存配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `initialSize` | `512` | 初始容量 |
| `maximumSize` | `2048` | 最大容量 |
| `expire` | `600` | 过期时间（秒） |
| `expireMode` | `WRITE` | 过期模式：`WRITE` 写入后计时 / `ACCESS` 最后访问后计时 |

### 3.5 @Remote — L2 缓存配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `expire` | `1800` | 固定过期时间（秒） |
| `randomBound` | `1200` | 随机过期偏移上限（秒） |
| `magnification` | `3` | 空值过期时间除数 |
| `preloadTime` | `300` | TTL 提前刷新时间（秒） |
| `enableRefresh` | `false` | 是否启用 TTL 异步刷新 |

> 实际过期时间 = `expire + random(0, randomBound)`，避免缓存雪崩。
> 空值过期时间 = `(expire + random) / magnification`，缩短空值缓存时间。

### 3.6 @CacheAction — 类级别默认配置

标注在类上，为该类所有缓存方法提供默认配置：

```java
@Service
@CacheAction(names = "user", keyGenerator = "userKeyGenerator")
public class UserService {

    @CacheResult(name = "user", key = "#userId")  // 继承类上的 keyGenerator
    public User getUser(Long userId) { ... }
}
```

> 优先级：方法注解 > 类注解 `@CacheAction` > 默认值

---

## 4. 执行顺序

单个方法上存在多个缓存注解时，执行顺序为：

```
1. 前置 @CacheEvict (beforeInvocation = true)  ← 先删缓存
2. @CacheResult                                ← 查缓存 / 回源加载
3. @CachePut                                   ← 更新缓存
4. 后置 @CacheEvict (beforeInvocation = false)  ← 后删缓存 + 延迟双删
```

---

## 5. 编程式 API

除了注解，也可注入 `CacheManager` 或 `CacheEvictPublisher` 编程操作：

```java
@Component
public class CacheService {

    private final CacheManager cacheManager;
    private final CacheEvictPublisher evictPublisher;

    public CacheService(CacheManager cacheManager, CacheEvictPublisher evictPublisher) {
        this.cacheManager = cacheManager;
        this.evictPublisher = evictPublisher;
    }

    public User getUser(Long userId) {
        Cache userCache = cacheManager.getAndAdd("user",
            CacheSettings.builder()
                .expire(1800)
                .randomBound(1200)
                .allowNullValue(true)
                .magnification(3)
                .build());
        User user = userCache.get(userId);
        if (user != null) return user;
        user = userRepository.findById(userId);
        userCache.put(userId, user);
        return user;
    }

    // 手动淘汰缓存
    public void evictUser(Long userId) {
        evictPublisher.publish(new CacheEvictEvent("user", userId));
    }
}
```

---

## 6. 延迟双删

### 6.1 适用场景

数据库读写分离 + 主从延迟场景下，标准的删除缓存流程存在时间窗口脏读风险：

```
线程 A: 写DB(主库) → 删缓存 → 返回
线程 B:                             查缓存(已删) → 查DB(从库, 旧数据) → 写缓存(旧数据)
```

延迟双删通过"立即删 + 延迟再删"消除此窗口。

### 6.2 工作流程

```
@CacheEvict 方法执行完成
  → TransactionalEventListener 监听到事务提交
    → 立即淘汰缓存 (doCacheEvict)
    → 提交延迟删除任务 (publishDelayEvict)
      → DelayQueue 在 delayEvictInterval 后触发
        → 二次淘汰缓存
```

### 6.3 配置

```yaml
com:
  cloud:
    cache:
      enable-delay-evict: true        # 开启延迟双删
      delay-evict-interval: 500       # 延迟时间（毫秒），建议大于主从延迟
      max-delay-evict-size: 10000     # 延迟队列容量上限
```

> 前置淘汰（`beforeInvocation = true`）不支持延迟双删，因为此时事务尚未提交。

---

## 7. 热 key 探测

### 7.1 原理

```
业务节点 (内嵌 Worker)
  ├── TurnKeyCollector (轮转桶统计算法)
  │     └── 统计每个 key 的访问频率
  ├── ScheduledPusherFactoryBean (定时上报)
  │     └── 上报统计数据到 Worker 集群
  ├── ReceiveNewKeySubscriber (热 key 订阅)
  │     └── 接收 Worker 推送的新热 key
  └── HotKeyCache (热 key 感知缓存)
        └── 热 key 数据自动获得 L1 加速
```

### 7.2 启用配置

```yaml
com:
  cloud:
    cache:
      hotkey:
        etcd-server: http://etcd1:2379,http://etcd2:2379
        app-name: my-application
```

### 7.3 依赖

需额外引入热 key 探测模块：

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-hotkey</artifactId>
</dependency>
```

---

## 8. 集群一致性

### 8.1 工作原理

```
节点 A 更新缓存
  → RedisRemoteCache.doPut(key, value)      // L2 写入
  → CaffeineLocalCache.doPut(key, value)    // 本节点 L1 直接更新
  → RefreshPolicy.sendEvict(name, key)      // Redis Pub/Sub 广播
      ↓
节点 B、C 收到 Redis 消息
  → CaffeineLocalCache.doEvict(key)         // 淘汰本地 L1
  → 下次 get 从 L2 重新加载
```

### 8.2 关键设计

- 本节点写入成功后 **直接更新 L1**（而非淘汰），避免下次 get 穿透
- `CacheNodePolicy` 生成唯一节点编号，Pub/Sub 消息去重（忽略本节点发出的消息）

---

## 9. L1 动态激活/卸载

运行时不重启应用即可动态开关 L1 本地缓存：

```java
@RestController
public class CacheController {

    private final CacheManager cacheManager;

    @PostMapping("/cache/{name}/local/activate")
    public String activateLocal(@PathVariable String name) {
        cacheManager.activateLocal(name);
        return "L1 activated for " + name;
    }

    @PostMapping("/cache/{name}/local/detach")
    public String detachLocal(@PathVariable String name) {
        cacheManager.detachLocal(name);
        return "L1 detached for " + name;
    }
}
```

**典型场景**：应用启动预热期关闭 L1，让流量直接打到 Redis 预热 L2；稳定后启用 L1 降低延迟。

---

## 10. 缓存预热

应用重启后首批请求命中空缓存，大量穿透到数据库造成 RT 毛刺。`architect-cache-warmup` 模块在启动时或定时主动填充缓存，消除冷启动延迟。

### 10.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-cache-warmup</artifactId>
</dependency>
```

> 要求 JDK 21+（虚拟线程），内部已传递依赖 `architect-cache-support`。

### 10.2 自动预热

**第一步：标记方法**

在已有的 `@CacheResult` 方法上加 `warmup = true`：

```java
@Service
public class HotDataService {

    @CacheResult(name = "user_cache", key = "#userId",
                 warmup = true, remark = "用户信息缓存，按用户ID预热")
    public User getUser(Long userId) {
        return userMapper.selectById(userId);
    }

    @CacheResult(name = "order_cache", key = "#orderId",
                 warmup = true, remark = "订单缓存")
    public Order getOrder(String orderId) { ... }
}
```

**第二步：配置预热参数**

```yaml
com:
  cloud:
    cache:
      warm-up:
        enabled: true
        timeout-seconds: 120
        tasks:
          user_cache:
            args:
              - [ 1001 ]
              - [ 1002 ]
          order_cache:
            args:
              - [ "ORDER_001" ]
              - [ "ORDER_002" ]
```

**第三步：观察启动日志**

应用启动后，`SmartInitializingSingleton` 回调自动扫描并预热，**不阻塞应用启动**：

```
[WarmUp] scanned 156 beans, registered 2 cache names
[WarmUp] starting auto warm-up, 2 caches
[WarmUp] ==== Cache Warm-Up Summary ====
[WarmUp] cache=user_cache success=2/2 duration=45ms
[WarmUp] cache=order_cache success=2/2 duration=67ms
[WarmUp] ==== Total: 2 tasks, 4 total, 4 success, 112ms ====
```

**执行流程**：

```
SmartInitializingSingleton.afterSingletonsInstantiated()
  → WarmUpScanner 扫描所有 @CacheResult(warmup=true)
  → WarmUpRegistry 按缓存名注册任务
  → WarmUpExecutor 虚拟线程并发执行
    → WarmUpCoordinator 获取分布式锁（按缓存名粒度）
    → method.invoke(bean, args) → AOP 拦截 → L2/L1 写入
    → CompletableFuture.allOf().orTimeout(timeoutSeconds)
```

每个 key 一个虚拟线程并发执行，200 个 key 的耗时趋近于最慢的那个 key。

### 10.3 手动预热（WarmUpTemplate）

注入 `WarmUpTemplate` 即可编程式触发预热，返回 `CompletableFuture<WarmUpResult>`：

```java
@Component
public class WarmUpJob {

    @Autowired
    private WarmUpTemplate warmUpTemplate;

    // 参数从 YAML 配置读取
    @Scheduled(cron = "0 0 6 * * ?")
    public void morningWarmUp() {
        WarmUpResult result = warmUpTemplate.warmUp("user_cache").join();
        log.info("warm-up done: {}/{} in {}ms",
                 result.getSuccessCount(), result.getTotalCount(), result.getDurationMs());
    }
}
```

**自定义参数**：

```java
List<Object[]> customArgs = List.of(
    new Object[]{"EAST", 1001L},
    new Object[]{"WEST", 1002L}
);
warmUpTemplate.warmUp("order_cache", customArgs)
    .thenAccept(result -> log.info("warm-up: {}/{}",
        result.getSuccessCount(), result.getTotalCount()));
```

**异步处理**：

```java
// 同步等待
WarmUpResult result = warmUpTemplate.warmUp("user_cache").join();

// 异步链式处理
warmUpTemplate.warmUp("user_cache")
    .thenAccept(r -> log.info("done: {}/{}", r.getSuccessCount(), r.getTotalCount()))
    .exceptionally(ex -> { log.error("failed", ex); return null; });

// 组合多个缓存并行预热
CompletableFuture.allOf(
    warmUpTemplate.warmUp("user_cache"),
    warmUpTemplate.warmUp("order_cache")
).join();
```

### 10.4 REST 端点

引入模块后自动暴露 Actuator 端点，路径前缀 `/actuator/warmup`：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/actuator/warmup/caches` | 查询所有可预热缓存的元数据 |
| `GET` | `/actuator/warmup/caches/{cacheName}` | 查询单个缓存的方法签名与示例参数 |
| `POST` | `/actuator/warmup/cache/{cacheName}` | 触发手动预热（可选自定义参数 body） |

**GET 查询元数据**：

```bash
# 列出所有可预热缓存
curl http://localhost:8080/actuator/warmup/caches

# 查看单个缓存详情
curl http://localhost:8080/actuator/warmup/caches/user_cache
```

响应示例：

```json
{
  "cacheName": "user_cache",
  "remark": "用户信息缓存，按用户ID预热",
  "methods": [
    { "beanName": "hotDataService", "methodName": "getUser", "paramTypes": ["Long"] }
  ],
  "sampleArgs": [[1001], [1002]]
}
```

**POST 触发预热**：

```bash
# 无 body → 使用 YAML 配置的参数
curl -X POST http://localhost:8080/actuator/warmup/cache/user_cache

# 自定义参数
curl -X POST http://localhost:8080/actuator/warmup/cache/user_cache \
  -H "Content-Type: application/json" \
  -d '[[1001], [1002], [1003]]'
```

响应示例：

```json
{
  "cacheName": "user_cache",
  "success": true,
  "totalCount": 3,
  "successCount": 3,
  "durationMs": 85
}
```

> 端点路径在 `/actuator` 下，天然复用 Spring Boot Actuator 安全体系。生产环境建议对 POST 加权限控制，或通过 `rest-endpoint: false` 关闭端点。

### 10.5 多实例部署

分布式锁按缓存名粒度协调（key 格式 `cache:warmup:lock:<cacheName>`），多实例自动互斥：

```
实例A: 获取锁 cache:warmup:lock:user_cache → 执行预热 → 释放
实例B: 尝试获取同一把锁 → 被跳过，记录 lock.skipped 指标
实例C: 同B
```

无 Redisson 时自动降级为单机模式，不影响功能。

### 10.6 配合 L1 动态切换

典型组合：预热期关闭 L1 让流量直接打到 Redis 预热 L2，稳定后启用 L1 降低延迟：

```java
// 预热前关闭 L1
cacheManager.detachLocal("user_cache");

// 执行预热 → 数据写入 L2（Redis）
warmUpTemplate.warmUp("user_cache").join();

// 预热完成后开启 L1
cacheManager.activateLocal("user_cache");
```

### 10.7 监控指标

预热过程自动上报 Micrometer 指标：

| 指标名 | 类型 | Tag | 说明 |
|--------|------|-----|------|
| `cache.warmup.total` | Counter | cache, status | 预热成功/失败条目数 |
| `cache.warmup.duration` | Timer | cache | 每次预热耗时 |
| `cache.warmup.lock.acquired` | Counter | cache | 锁获取成功次数 |
| `cache.warmup.lock.skipped` | Counter | cache | 锁跳过次数（其他节点执行中） |

### 10.8 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `com.cloud.cache.warm-up.enabled` | `true` | 是否启用自动预热 |
| `com.cloud.cache.warm-up.fail-fast` | `false` | 预热失败是否阻止应用启动 |
| `com.cloud.cache.warm-up.timeout-seconds` | `120` | 预热总超时（秒），超时后聚合已完成结果 |
| `com.cloud.cache.warm-up.lock-wait-seconds` | `30` | 分布式锁等待超时（秒） |
| `com.cloud.cache.warm-up.caches` | (空=全部) | 要执行的缓存名列表 |
| `com.cloud.cache.warm-up.rest-endpoint` | `true` | 是否暴露 `/actuator/warmup` 端点 |
| `com.cloud.cache.warm-up.tasks.<name>.args` | — | 预热参数，二维数组，内层为一次方法调用的参数列表 |

---

## 11. 监控指标

### 11.1 启用

```yaml
com:
  cloud:
    cache:
      enable-metric: true
```

### 11.2 指标项

| 指标 | 说明 |
|------|------|
| `cache.hits` | L1 命中次数 |
| `cache.misses` | 未命中次数 |
| `cache.evictions` | 淘汰次数 |
| `cache.load.success` | 加载成功次数 |
| `cache.load.fail` | 加载失败次数 |
| `cache.load.time` | 加载耗时 |
| `cache.size` | 缓存条目数 |

> 指标通过 Micrometer 暴露，可直接接入 Prometheus + Grafana。

---

## 12. 扩展点

所有扩展点均通过 `@ConditionalOnMissingBean` 实现，用户只需注册同名 Bean 即可覆盖：

| 接口 | 说明 | 默认实现 |
|------|------|---------|
| `KeyGenerator` | 缓存 Key 生成策略 | `SimpleKeyGenerator` |
| `CacheResolver` | 解析缓存实例 | `SimpleCacheResolver` |
| `CacheErrorHandler` | 缓存异常处理 | `SimpleCacheErrorHandler` (静默降级) |
| `CacheRedisSupplier` | Redis 客户端供应 | `DefaultCacheRedisSupplier` |
| `CacheNodePolicy` | 集群节点标识策略 | `RandomNodePolicy` |
| `RefreshPolicy` | 集群缓存失效策略 | `RedisTopicRefreshPolicy` |

**自定义示例**：

```java
@Component
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(Exception exception, Cache cache, Object key) {
        // 自定义告警逻辑
        alertService.send("缓存读取失败: " + cache.getName());
    }

    // ... 其他方法
}
```

---

## 13. SpEL 表达式上下文

注解中可用的 SpEL 变量：

| 变量 | 说明 | 适用注解 |
|------|------|---------|
| `#参数名` | 方法参数值，如 `#userId`、`#user.id` | 全部 |
| `#result` | 方法返回值 | `@CachePut` (unless)、`@CacheResult` (unless) |

**常用表达式**：

```java
// 参数属性
@CacheResult(name = "user", key = "#user.id")
public User getUser(User user) { ... }

// 多参数组合
@CacheResult(name = "order", key = "#userId + ':' + #orderId")
public Order getOrder(Long userId, Long orderId) { ... }

// 条件缓存
@CacheResult(name = "user", key = "#userId", condition = "#userId > 0")
public User getUser(Long userId) { ... }

// 排除 null 结果
@CacheResult(name = "user", key = "#userId", unless = "#result == null")
public User getUser(Long userId) { ... }
```

---

## 14. 配置参考

### 14.1 全部配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `com.cloud.cache.refresh-topic` | `cache:refresh:default` | 集群失效广播 Topic |
| `com.cloud.cache.enable-local` | `true` | 是否启用 L1 本地缓存 |
| `com.cloud.cache.enable-delay-evict` | `true` | 是否启用延迟双删 |
| `com.cloud.cache.delay-evict-interval` | `500` | 延迟删除间隔（毫秒） |
| `com.cloud.cache.max-delay-evict-size` | `10000` | 延迟删除队列容量上限 |
| `com.cloud.cache.allow-null-value` | `true` | 是否缓存 null 值防穿透 |
| `com.cloud.cache.enable-metric` | `false` | 是否启用 Micrometer 指标 |
| `com.cloud.cache.ttl-refresh-interval` | `-60` | TTL 刷新检查间隔（秒） |
| `com.cloud.cache.only-public` | `true` | 是否仅代理 public 方法 |
| `com.cloud.cache.hotkey.etcd-server` | — | 热 key 探测 Etcd 地址 |
| `com.cloud.cache.warm-up.enabled` | `true` | 是否启用自动预热 |
| `com.cloud.cache.warm-up.timeout-seconds` | `120` | 预热总超时（秒） |
| `com.cloud.cache.warm-up.rest-endpoint` | `true` | 是否暴露 `/actuator/warmup` 端点 |
| `com.cloud.cache.warm-up.tasks.<name>.args` | — | 预热参数，二维数组，内层为一次方法调用的参数列表 |

### 14.2 过期时间计算

- **正常数据**：`expire + random(0, randomBound)`，默认 1800~3000 秒
- **空值数据**：`(expire + random) / magnification`，默认 600~1000 秒

---

## 15. 最佳实践

### 15.1 缓存 Key 设计

```
推荐格式：业务前缀:实体名:业务ID
示例：user:1001, product:50023, config:site_settings
```

### 15.2 L1 容量规划

```
L1.maximumSize = 热点 key 数量 × 2

例如：200 个高频商品
  → maximumSize = 400
  → 内存占用 ≈ 400 × 2KB = 800KB
```

### 15.3 过期时间建议

| 数据特征 | `expire` | `randomBound` | `enableRefresh` |
|---------|---------|---------------|-----------------|
| 可变配置 | 600~1800 | 300~600 | false |
| 热点数据 | 1800~7200 | 600~1200 | **true** |
| 静态字典 | 7200+ | 1200+ | false |
| 用户 Session | 600~1800 | 300 | false |

### 15.4 哪些场景开启 L1

```
适合 L1：
  - 读多写少的热点数据（配置字典、商品详情）
  - 对延迟敏感（Caffeine <1ms）
  - 批量查询后的单条回查

不适合 L1：
  - 写入频繁的数据（频繁失效）
  - 数据量极大（可能撑爆本地内存）
  - 一致性要求极高（Pub/Sub 有极低延迟）
```

### 15.5 事务场景

```java
// @CacheEvict 配合 @Transactional 自动启用延迟双删
@Transactional
@CacheEvict(name = "user", key = "#userId")
public void updateUser(Long userId, UserUpdateDto dto) {
    userRepository.update(userId, dto);
}
// 事务提交后：立即删缓存 → 500ms 后再删一次
```

---

## 16. 常见问题

**Q: 缓存穿透、击穿、雪崩如何防护？**

- **穿透**：`allowNullValue = true` 时 null 值编码为 `NullValue` 缓存，空值过期更短
- **击穿**：L1 未命中时自动加 key 级锁（`KEY_LOCKS`），同一 key 仅一个线程穿透到 L2
- **雪崩**：`expire + random(0, randomBound)` 随机分散过期时间

**Q: 为何延迟双删用 `DelayQueue` 而不是 `ScheduledExecutorService`？**

`DelayQueue` + 单消费线程的方案更轻量，延迟任务数量通常不大，且 `take()` 无任务时零 CPU 消耗。

**Q: `enableLocal = true` 但没配 `refreshTopic` 会怎样？**

`refreshTopic` 默认值为 `cache:refresh:default`，无需手动配置。仅在多集群共用同一 Redis 需要隔离时才需自定义。

**Q: 缓存异常会影响业务吗？**

不会。默认 `SimpleCacheErrorHandler` 仅记 warn 日志静默降级，缓存故障不影响业务方法执行。

**Q: 如何自定义异常处理？**

实现 `CacheErrorHandler` 接口并注册为 Spring Bean，可自定义告警、降级、或恢复抛异常等策略。

**Q: 注解和编程式 API 如何选择？**

- 注解适合 95% 的场景：标准 CRUD 方法返回值缓存
- 编程式 API 适合：动态 key 列表、非方法边界缓存、条件复杂的缓存逻辑

**Q: 性能如何？**

- L1 命中 (Caffeine)：< 1ms
- L2 命中 (Redis)：1~5ms
- 回源加载：DB 查询耗时 + L2 写入耗时
- AOP 开销：SpEL 评估 ≈ 0.01ms，元数据缓存命中后基本无开销

**Q: 缓存预热和 TTL 异步刷新有什么区别？**

- **预热**：应用启动时或手动触发，批量填充缓存，消除冷启动延迟。适合启动后立即需要的高频数据
- **TTL 刷新**：运行时自动续期，防止热点 key 过期。适合持续高访问的热点数据
- 两者互补：预热负责"冷启动"，TTL 刷新负责"运行中保温"

**Q: 预热会阻塞应用启动吗？**

不会。预热在后台线程中执行（`SmartInitializingSingleton` 回调 + `CacheThreadPoolExecutor`），应用主流程不等待预热完成即可接收请求。预热期间的请求走正常的懒加载路径。

**Q: 如何确认预热是否生效？**

1. 查看启动日志中的 `[WarmUp] ==== Cache Warm-Up Summary ====` 摘要
2. 调用 `GET /actuator/warmup/caches` 确认可预热列表是否包含目标缓存
3. 检查 Micrometer 指标 `cache.warmup.total` 的 success 计数
4. 预热后首次请求日志中不应出现缓存加载日志（说明已命中）

**Q: YAML 配置的参数数量和类型如何确定？**

调用 `GET /actuator/warmup/caches/{cacheName}` 查看 `methods[].paramTypes`，参数类型必须与之一一对应。Spring `ConversionService` 会自动将 YAML 值转换为目标类型（如字符串 `"1001"` 自动转 `Long`）。

---

## 17. 与 Spring Cache 对比

| 特性 | architect-cache | Spring Cache |
|------|:---:|:---:|
| L1+L2 二级缓存 | ✅ | ❌ |
| 延迟双删 | ✅ | ❌ |
| 热 key 探测 | ✅ | ❌ |
| L1 动态开关 | ✅ | ❌ |
| TTL 异步刷新 | ✅ | ❌ |
| 缓存预热 | ✅ | ❌ |
| 集群一致性 | ✅ Redis Pub/Sub | ❌ |
| 缓存穿透防护 | ✅ 空值编码 | ❌ 需手动实现 |
| 注解数量 | 3 个 | 4 个 |
| 学习成本 | 中 | 低 |
| 社区生态 | 内部项目 | Spring 官方 |
