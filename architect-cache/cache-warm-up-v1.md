# 缓存预热方案设计 v1

## 1. 背景与目标

当前架构采用 L1（Caffeine）+ L2（Redisson）双层缓存，数据在首次请求时按需加载（懒加载）。存在两个问题：

1. **冷启动延迟**：应用重启后首批请求命中空缓存，大量请求穿透到数据库，造成 RT 毛刺
2. **定时刷新不确定性**：依赖 `@Remote.preloadTime` 的 TTL 自动续期，无法精确控制在指定时间窗口预热

**目标**：实现手动 + 自动双模式预热，统一走 `@CacheResult` → AOP 拦截器 → 缓存写入，按缓存名驱动。

---

## 2. 使用方式

### 2.1 快速开始

**第一步：引入依赖**

```xml

<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-cache-warmup</artifactId>
</dependency>
```

**第二步：标注方法**

在已有的 `@CacheResult` 方法上加 `warmup=true`：

```java

@Service
public class HotDataService {

    @CacheResult(names = "user_cache", key = "#userId",
            warmup = true, remark = "用户信息缓存，按用户ID预热")
    public User getUser(Long userId) {
        return userMapper.selectById(userId);
    }
}
```

**第三步：配置参数**

```yaml
com.cloud.cache.warm-up:
  enabled: true
  tasks:
    user_cache:
      args:
        - [ 1001 ]    # 启动后自动预热 userId=1001
        - [ 1002 ]
```

启动应用后，`SmartInitializingSingleton` 回调自动触发扫描和预热，无需额外代码。

### 2.2 自动预热

应用启动时自动执行，适合冷启动后快速填充热点数据。

```java
// 声明可预热的方法
@CacheResult(names = "order_cache", key = "#orderId",
        warmup = true, remark = "订单缓存")
public Order getOrder(String orderId) { ...}
```

```yaml
# 配置要预热的参数
com.cloud.cache.warm-up:
  timeout-seconds: 120       # 预热总超时
  caches:                    # 空 = 全部执行，可指定 ["order_cache"]
  tasks:
    order_cache:
      args:
        - [ "ORDER_001" ]
        - [ "ORDER_002" ]
        - [ "ORDER_003" ]
```

启动日志输出：

```
[WarmUp] scanned 156 beans, registered 2 cache names
[WarmUp] starting auto warm-up, 2 caches
[WarmUp] ==== Cache Warm-Up Summary ====
[WarmUp] cache=user_cache success=2/2 duration=45ms
[WarmUp] cache=order_cache success=3/3 duration=67ms
[WarmUp] ==== Total: 2 tasks, 5 total, 5 success, 112ms ====
```

所有 key 通过虚拟线程并发执行，200 个 key 耗时趋近于最慢的那个。

### 2.3 手动预热

#### 定时任务

```java

@Component
public class WarmUpJob {

    @Autowired
    private WarmUpTemplate warmUpTemplate;

    @Scheduled(cron = "0 0 6 * * ?")          // 每天 6:00
    public void morningWarmUp() {
        warmUpTemplate.warmUp("user_cache")    // 参数从 YAML 配置读取
                      .thenAccept(result -> log.info("morning warm-up: {}/{} in {}ms",
                                                     result.getSuccessCount(),
                                                     result.getTotalCount(),
                                                     result.getDurationMs()));
    }
}
```

#### 自定义参数

```java
List<Object[]> customArgs = List.of(
        new Object[]{"EAST", 1001L},
        new Object[]{"WEST", 1002L}
);
warmUpTemplate.

warmUp("order_cache",customArgs)
        .

thenAccept(result ->{...});
```

#### REST 端点

三个端点，接口细节见 [6.3 REST 端点](#63-rest-端点) 和 [7. 缓存预热元数据视图](#7-缓存预热元数据视图)。

```bash
# 查询所有可预热缓存及参数格式
GET /actuator/warmup/caches

# 查询单个缓存详情（方法签名 + 示例参数）
GET /actuator/warmup/caches/order_cache

# 执行手动预热（使用 YAML 配置的参数）
POST /actuator/warmup/cache/order_cache

# 执行手动预热（自定义参数）
curl -X POST /actuator/warmup/cache/order_cache \
  -H "Content-Type: application/json" \
  -d '[["ORDER_001"], ["ORDER_002"], ["ORDER_003"]]'
```

响应示例（POST）：

```json
{
  "cacheName": "order_cache",
  "success": true,
  "totalCount": 3,
  "successCount": 3,
  "durationMs": 67
}
```

GET 查询元数据响应示例：

```json
{
  "cacheName": "order_cache",
  "remark": "订单缓存",
  "methods": [
    {
      "beanName": "OrderService",
      "methodName": "getOrder",
      "paramTypes": [
        "String"
      ]
    }
  ],
  "sampleArgs": [
    [
      "ORDER_001"
    ],
    [
      "ORDER_002"
    ]
  ]
}
```

拿到 `paramTypes` 和 `sampleArgs` 后，调用方就知道如何构造 `POST` 的
body，形成 [查询 → 执行配套链路](#73-查询--执行配套链路)。

### 2.4 异步处理

`warmUp()` 返回 `CompletableFuture<WarmUpResult>`，调用方决定同步还是异步：

```java
// 同步等待
WarmUpResult result = warmUpTemplate.warmUp("user_cache").join();

// 异步链式
warmUpTemplate.

warmUp("user_cache")
        .

thenAccept(r ->log.

info("done: {}/{}",r.getSuccessCount(),r.

getTotalCount()))
        .

exceptionally(ex ->{log.

error("failed",ex); return null;});

// 组合多个缓存并行预热
        CompletableFuture.

allOf(
        warmUpTemplate.warmUp("user_cache"),
    warmUpTemplate.

warmUp("order_cache")
).

join();
```

### 2.5 多实例部署

分布式锁按缓存名粒度协调，多实例自动互斥：

```
实例A: 获取锁 cache:warmup:lock:user_cache → 执行预热 → 释放
实例B: 尝试获取同一把锁 → 被跳过，记录 lock.skipped 指标
实例C: 同B
```

无 Redisson 时自动降级为单机模式，不影响功能。

---

## 3. 核心架构

```
┌────────────────────────────────────────────────────────────┐
│                   应用层 / 运维平台                          │
│  @CacheResult(warmup=true)  │  WarmUpTemplate API          │
│                             │  REST /actuator/warmup       │
└──────────────┬─────────────────────────────────────────────┘
               │
               ▼
┌────────────────────────────────────────────────────────────┐
│                 WarmUpEngine（预热引擎）                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐ │
│  │ WarmUpScanner │  │WarmUpExecutor│  │ WarmUpCoordinator │ │
│  │ (扫描        │  │ (虚拟线程并发) │  │ (分布式协调锁)      │ │
│  │  @CacheResult │  │              │  │                   │ │
│  │  warmup=true) │  └──────┬───────┘  └───────────────────┘ │
│  └──────────────┘          │                                 │
│               ┌────────────▼──────────┐                      │
│               │ WarmUpArgsProvider     │ ← YAML / 配置中心     │
│               │ (参数来源抽象)           │                      │
│               └───────────────────────┘                      │
│  ┌────────────────────────────────────┐                     │
│  │ WarmUpMeta / MethodMeta (元数据视图) │ ← 查询接口           │
│  └────────────────────────────────────┘                     │
└──────────────┬─────────────────────────────────────────────┘
               │
               ▼
┌────────────────────────────────────────────────────────────┐
│           现有缓存基础设施（复用，不改动）                       │
│  CacheManager  │  CacheOperationSource  │  Cache.get(key)   │
│  @CacheResult AOP 拦截器  │  SpEL 表达式解析                 │
└────────────────────────────────────────────────────────────┘
```

设计原则：

- **独立模块**：`architect-cache-warmup`，与缓存核心解耦，按需引入
- **不改造现有缓存核心类**：仅给 `@CacheResult` 加 `warmup`/`remark` 属性（增量、向后兼容）
- **零注解入侵**：开发只需在已有的 `@CacheResult` 上加 `warmup=true`
- **按缓存名驱动**：统一以缓存名作为预热标识，自动/手动/配置三个维度对齐
- **虚拟线程并发**：JDK 21+ 虚拟线程，每个 key 独立线程并发执行，I/O 密集型场景提速 200x

---

## 4. @CacheResult 预热标记

```java
public @interface CacheResult {
    // ... 现有属性不变 ...

    /**
     * 是否允许缓存预热，默认 false。
     * 仅作用于预热扫描器，对 AOP 缓存拦截无影响。
     */
    boolean warmup() default false;

    /**
     * 缓存备注，配合 warmup 使用，说明该缓存的业务场景。
     * 查询缓存预热元数据时可看到该缓存的业务含义。
     */
    String remark() default "";
}
```

---

## 5. 自动预热模式

### 5.1 使用示例

```java

@Service
public class HotDataService {

    // 开发：声明该方法可预热，附带业务说明
    @CacheResult(names = "user_cache", key = "#userId",
            warmup = true, remark = "用户信息缓存，按用户ID预热")
    public User getUser(Long userId) { ...}

    // 组合 key 天然支持
    @CacheResult(names = "user_cache", key = "#region + ':' + #userId",
            warmup = true, remark = "用户信息缓存")
    public User getUserByRegion(String region, Long userId) { ...}
}
```

### 5.2 配置

```yaml
com.cloud.cache.warm-up:
  enabled: true
  fail-fast: false
  timeout-seconds: 120          # 预热总超时（秒），超时后聚合已完成结果
  lock-wait-seconds: 30         # 分布式锁等待超时（秒）
  caches: [ ]                    # 要执行的缓存名，空 = 全部执行
  tasks:
    user_cache: # 对应 @CacheResult(names="user_cache")
      args:
        - [ 1001 ]
        - [ 1002 ]
    order_cache:
      args:
        - [ "ORDER_123" ]
```

### 5.3 启动扫描与执行流程

```
SmartInitializingSingleton.afterSingletonsInstantiated()
    │
    ▼
WarmUpScanner.scan()
    │  遍历所有 Bean → MethodIntrospector.selectMethods
    │  筛选 @CacheResult(warmup=true) 的方法
    │
    ▼
WarmUpRegistry.register(cacheName, task)
    │  按 @CacheResult.names() 的每个缓存名注册
    │
    ▼  后台线程执行（不阻塞启动）
WarmUpExecutor.execute(cacheName, args, tasks) → CompletableFuture<WarmUpResult>
    │  每个 key 一个虚拟线程并发执行:
    │  CompletableFuture.runAsync(() -> {
    │      方法.invoke(bean, convertedArgs)
    │      ├─ @CacheResult AOP → Cache.get(key, callable) → 缓存写入
    │  }, virtualThreadExecutor)
    │  CompletableFuture.allOf(futures).orTimeout(timeoutSeconds)
    │  聚合所有 key 结果 → WarmUpResult
    │  锁在 whenComplete 中释放
    │
    ▼
WarmUpMetrics.report() → Micrometer 指标 + 日志摘要
```

### 5.4 虚拟线程并发模型

- 每个预热 key 提交到 `Executors.newVirtualThreadPerTaskExecutor()` 独立执行
- `AtomicInteger` 累加成功计数，线程安全
- `CompletableFuture.allOf().orTimeout()` 控制总超时，超时后仍聚合已完成结果
- 虚拟线程 I/O 阻塞时自动 yield，200 个 key 几乎零开销
- 耗时从 `N × 单key耗时` 降为 `max(单key耗时)`，典型场景提速 200x

### 5.5 参数类型转换

YAML 配置中的参数通过 Spring `ConversionService` 自动转换为方法参数类型：

```yaml
args:
  - [ "EAST", 1001 ]    # 1001 是数字 → 自动转 Long
  - [ "WEST", "1002" ]  # "1002" 是字符串 → 也自动转 Long
```

内部使用 `DefaultConversionService.getSharedInstance()` 复用 JVM 级别单例。

### 5.6 多方法匹配同一个缓存名

一个缓存名可能对应多个 `@CacheResult` 方法（如 `getUser(id)` 和 `getUserByRegion(region, id)`）。执行时按参数个数匹配第一个合适的方法调用，AOP
一次性写入所有关联缓存。

---

## 6. 手动预热模式

### 6.1 WarmUpTemplate API

```java
public class WarmUpTemplate {

    /** 按缓存名预热，参数从 YAML 配置读取 */
    public CompletableFuture<WarmUpResult> warmUp(String cacheName);

    /** 按缓存名预热，参数由调用方提供 */
    public CompletableFuture<WarmUpResult> warmUp(String cacheName, List<Object[]> args);

    /** 获取所有可预热缓存的元数据 */
    public List<WarmUpMeta> metas();

    /** 获取单个缓存名的元数据 */
    public WarmUpMeta meta(String cacheName);
}
```

### 6.2 使用示例

```java
// ── 场景1：定时任务（同步等待） ──
@Component
public class WarmUpJob {

    @Autowired
    private WarmUpTemplate warmUpTemplate;

    @Scheduled(cron = "0 0 6 * * ?")
    public void morningWarmUp() {
        WarmUpResult result = warmUpTemplate.warmUp("user_cache").join();
        log.info("morning warm-up: success={}/{} duration={}ms",
                 result.getSuccessCount(), result.getTotalCount(), result.getDurationMs());
    }
}

// ── 场景2：异步链式处理 ──
warmUpTemplate.

warmUp("user_cache")
    .

thenAccept(result ->log.

info("warm-up done: {}/{}",result.getSuccessCount(),result.

getTotalCount()));

// ── 场景3：调用方自传参数 ──
List<Object[]> customArgs = List.of(
        new Object[]{"EAST", 1001L},
        new Object[]{"WEST", 1002L}
);
CompletableFuture<WarmUpResult> future = warmUpTemplate.warmUp("user_cache", customArgs);
```

### 6.3 REST 端点

端点前缀 `/actuator/warmup`，对标 Spring Boot Actuator 安全体系，由 `com.cloud.cache.warm-up.rest-endpoint` 控制开关（默认开启）。

#### 6.3.1 端点一览

| 方法   | 路径                                    | 说明               | 异步 |
|------|---------------------------------------|------------------|----|
| GET  | `/actuator/warmup/caches`             | 查询所有可预热缓存的元数据列表  | 否  |
| GET  | `/actuator/warmup/caches/{cacheName}` | 查询单个缓存的方法签名与示例参数 | 否  |
| POST | `/actuator/warmup/cache/{cacheName}`  | 触发手动预热（可选自定义参数）  | 是  |

#### 6.3.2 端点配置

```yaml
com.cloud.cache.warm-up:
  rest-endpoint: true   # 默认 true，设为 false 则整个 WarmUpEndpoint Bean 不注册
```

关闭后 `WarmUpEndpoint` 不会被 Spring 容器加载（`@ConditionalOnProperty` 控制）。

#### 6.3.3 POST 触发预热

```
POST /actuator/warmup/cache/{cacheName}
Content-Type: application/json
```

**请求体规则：**

- **无 body 或空数组** → 使用 YAML 配置 `tasks.<cacheName>.args` 作为预热参数
- **有 body** → 使用调用方提供的参数，格式 `[[arg1, arg2], [arg1, arg2], ...]`，每层数组是一个方法调用的参数列表

**请求示例：**

```bash
# 场景1：使用 YAML 配置的参数预热（body 为空）
curl -X POST http://localhost:8080/actuator/warmup/cache/user_cache

# 场景2：自定义参数预热（组合 key）
curl -X POST http://localhost:8080/actuator/warmup/cache/user_cache \
  -H "Content-Type: application/json" \
  -d '[["EAST", 1001], ["WEST", 1002]]'

# 场景3：单参数方法
curl -X POST http://localhost:8080/actuator/warmup/cache/order_cache \
  -H "Content-Type: application/json" \
  -d '[["ORDER_001"], ["ORDER_002"], ["ORDER_003"]]'
```

**响应体：`CompletableFuture<WarmUpResult>`**

Spring MVC 在 `UniformResponseBodyAdvice` 异步支持下自动等待 Future 完成后写入 HTTP 响应（`architect-webmvc`
模块提供异步返回值包装能力）。

```json
{
  "cacheName": "user_cache",
  "beanName": null,
  "methodName": null,
  "success": true,
  "totalCount": 2,
  "successCount": 2,
  "durationMs": 67,
  "errorMessage": null
}
```

**内部处理链路：**

```
POST /actuator/warmup/cache/user_cache  Body: [["EAST", 1001], ["WEST", 1002]]
    │
    ▼
WarmUpEndpoint.warmUpCache("user_cache", args)
    │  args 为空 → warmUpTemplate.warmUp(cacheName)       ← 从 YAML 读参数
    │  args 非空 → warmUpTemplate.warmUp(cacheName, args)  ← 用调用方参数
    │
    ▼
WarmUpTemplate.warmUp(cacheName, argsList)
    │  1. 从 WarmUpRegistry 查找该 cacheName 的关联 Task 列表
    │  2. 无匹配 → 日志 warn + 返回 CompletableFuture.completedFuture(null)
    │  3. 委托 WarmUpExecutor.execute(cacheName, args, tasks)
    │
    ▼
WarmUpExecutor.execute()
    │  1. 尝试获取分布式锁 cache:warmup:lock:<cacheName>
    │     - 失败 → 日志 info "skip, another node handling" → 返回 null
    │  2. 每个 arg → Virtual Thread → Method.invoke(bean, args)
    │     - @CacheResult AOP 拦截 → Cache.get(key, callable)
    │     - L2（RedisRemoteCache）写入 → L1（CaffeineLocalCache）写入
    │  3. CompletableFuture.allOf(futures).orTimeout(timeoutSeconds)
    │  4. whenComplete 回调中释放分布式锁
    │
    ▼
CompletableFuture<WarmUpResult> → Spring MVC → 写入 HTTP Response
```

**HTTP 状态码与响应语义：**

| 状态码 | 响应 body        | 场景           | 说明                                                |
|-----|----------------|--------------|---------------------------------------------------|
| 200 | `WarmUpResult` | 预热正常完成       | `success=true`，`totalCount`/`successCount` 反映实际结果 |
| 200 | `null`         | 缓存名无注册方法     | `WarmUpTemplate` 日志 warn 级别                       |
| 200 | `null`         | 分布式锁被其他节点持有  | `WarmUpExecutor` 日志 info 级别 skip                  |
| 200 | `null`         | 无配置参数且调用方未传参 | `WarmUpExecutor` 日志 info 级别 skip                  |
| 200 | `WarmUpResult` | 预热触发但部分超时    | `success=true`，`successCount` ≤ `totalCount`      |

> **设计决策：** 所有"非异常"场景统一返回 HTTP 200，通过 `WarmUpResult` 字段和 `null` 值区分具体状态。预热是**非关键路径操作
**，调用方不应因预热异常而触发 HTTP 层面的重试或告警。

**超时行为：**

`CompletableFuture.allOf(futures).orTimeout(timeoutSeconds)` —— 超时后 Future 以 `TimeoutException` 完成，`handle()`
中捕获异常并聚合已完成结果：

- 100 个 key 超时前已完成 73 个 → `totalCount=100, successCount=73, success=true`
- 已完成的 73 个 key 已写入 L2/L1 缓存，可正常使用
- 剩余 27 个 key 对应的缓存条目留待后续请求懒加载

#### 6.3.4 GET 查询元数据

```bash
# 列出所有可预热缓存
curl http://localhost:8080/actuator/warmup/caches

# 查看单个缓存详情（方法签名 + 示例参数）
curl http://localhost:8080/actuator/warmup/caches/user_cache
```

响应结构和典型使用流程见 [7. 缓存预热元数据视图](#7-缓存预热元数据视图)。

#### 6.3.5 端点安全

- 路径在 `/actuator` 下，天然复用 Spring Boot Actuator 安全体系：
    - Spring Security 可基于 `management.endpoints.web.base-path` 统一管控
    - 支持 `management.endpoint.health.roles` 等标准角色配置扩展
- **生产环境建议：**
    - 对 `/actuator/warmup/**` 加权限控制，GET 允许只读角色，POST 限管理员角色
    - 可通过 `com.cloud.cache.warm-up.rest-endpoint=false` 彻底关闭端点
- **内网部署：** 配合 `management.server.port` 将 Actuator 暴露在独立管理端口（如 8081），与业务端口隔离

#### 6.3.6 端点源码

```java

@ApiBody
@RestController
@RequestMapping("/actuator/warmup")
public class WarmUpEndpoint {

    private final WarmUpTemplate warmUpTemplate;

    public WarmUpEndpoint(WarmUpTemplate warmUpTemplate) {
        this.warmUpTemplate = warmUpTemplate;
    }

    @PostMapping(value = "/cache/{cacheName}")
    public CompletableFuture<WarmUpResult> warmUpCache(
            @PathVariable String cacheName,
            @RequestBody(required = false) List<List<Object>> args) {
        if (CollectionUtils.isEmpty(args)) {
            return warmUpTemplate.warmUp(cacheName);
        }
        List<Object[]> argsList = args.stream().map(List::toArray).toList();
        return warmUpTemplate.warmUp(cacheName, argsList);
    }

    @GetMapping(value = "/caches")
    public List<WarmUpMeta> getCaches() {
        return warmUpTemplate.metas();
    }

    @GetMapping(value = "/caches/{cacheName}")
    public WarmUpMeta getCache(@PathVariable String cacheName) {
        return warmUpTemplate.meta(cacheName);
    }
}
```

### 6.4 WarmUpResult

```java

@Data
public class WarmUpResult {
    private String  cacheName;    // 缓存名
    private String  beanName;     // 目标 Bean（仅自动预热时填充，手动调用为 null）
    private String  methodName;   // 目标方法（仅自动预热时填充，手动调用为 null）
    private boolean success;      // 流程是否正常完成（异常才为 false）
    private long    durationMs;   // 总耗时（毫秒）
    private String  errorMessage; // 异常信息（success=false 时有值）
    private int     totalCount;   // 参数总数
    private int     successCount; // 成功个数（成功写入缓存的 key 数量）
}
```

- `success` 语义：流程不抛异常即为 true，个别 key 失败不影响
- `beanName` / `methodName`：仅自动预热时填充（`WarmUpScanner` 有具体 Bean 信息），手动调用为 null
- 批量场景：`totalCount` / `successCount` 体现逐条统计

---

## 7. 缓存预热元数据视图

对外暴露"哪些缓存可预热"以及"参数签名"，与手动预热接口形成"查询 → 执行"配套链路。

### 7.1 WarmUpMeta（缓存级别）

```java

@Data
public class WarmUpMeta {
    private String           cacheName;   // 缓存名
    private String           remark;      // 业务备注（来自 @CacheResult.remark）
    private List<MethodMeta> methods;     // 关联的方法列表
    private List<Object[]>   sampleArgs;  // YAML 配置的示例参数（无则为 []）
}
```

### 7.2 MethodMeta（方法级别）

```java

@Data
public class MethodMeta {
    private String   beanName;    // 目标 Bean 名
    private String   methodName;  // 方法名
    private String[] paramTypes;  // 参数类型简名 Class.getSimpleName()
}
```

### 7.3 「查询 → 执行」配套链路

元数据查询与手动预热接口形成完整的配套流程，运维/调用方无需翻阅代码即可完成预热操作。

#### 步骤 1：查询可预热缓存列表

```bash
GET /actuator/warmup/caches
```

响应：

```json
[
  {
    "cacheName": "user_cache",
    "remark": "用户信息缓存，按用户ID预热",
    "methods": [
      {
        "beanName": "hotDataService",
        "methodName": "getUser",
        "paramTypes": [
          "Long"
        ]
      }
    ],
    "sampleArgs": [
      [
        1001
      ],
      [
        1002
      ]
    ]
  },
  {
    "cacheName": "order_cache",
    "remark": "订单缓存",
    "methods": [
      {
        "beanName": "orderService",
        "methodName": "getOrder",
        "paramTypes": [
          "String"
        ]
      }
    ],
    "sampleArgs": [
      [
        "ORDER_001"
      ],
      [
        "ORDER_002"
      ]
    ]
  }
]
```

**关键信息解读：**

| 字段                     | 用途                          | 示例                                             |
|------------------------|-----------------------------|------------------------------------------------|
| `cacheName`            | POST 预热时的路径变量 `{cacheName}` | `user_cache`                                   |
| `remark`               | 业务含义，帮助确认缓存用途               | "用户信息缓存，按用户ID预热"                               |
| `methods[].paramTypes` | 参数类型声明，构造 POST body 时必须类型匹配 | `["String", "Long"]` → body 中每项为 `["str", 数字]` |
| `sampleArgs`           | YAML 配置的示例参数，**格式可直接复用**    | `[[1001],[1002]]`                              |

#### 步骤 2：构造参数并执行预热

拿到 `paramTypes` 和 `sampleArgs` 后，调用方按相同格式构造参数：

```bash
# 照 sampleArgs 格式构造参数，预热 userId=1001, 1002, 1003
curl -X POST http://localhost:8080/actuator/warmup/cache/user_cache \
  -H "Content-Type: application/json" \
  -d '[[1001], [1002], [1003]]'
```

响应：

```json
{
  "cacheName": "user_cache",
  "success": true,
  "totalCount": 3,
  "successCount": 3,
  "durationMs": 85
}
```

#### 步骤 3：批量预热多个缓存

```bash
# 配合脚本批量操作
for cache in user_cache order_cache; do
  curl -s -X POST "http://localhost:8080/actuator/warmup/cache/$cache"
done
```

或使用 `WarmUpTemplate` API 并行预热：

```java
CompletableFuture.allOf(
        warmUpTemplate.warmUp("user_cache"),
    warmUpTemplate.

warmUp("order_cache")
).

join();
```

#### 完整集成流程

```
运维平台/脚本
    │
    ├─(1)─ GET /actuator/warmup/caches
    │       → 展示所有可预热缓存及参数格式
    │
    ├─(2)─ GET /actuator/warmup/caches/user_cache
    │       → 确认 paramTypes=["Long"]，sampleArgs=[[1001],[1002]]
    │
    ├─(3)─ POST /actuator/warmup/cache/user_cache  Body: [[1001], [1002], [1003]]
    │       → 异步预热 3 个 key
    │       → 分布式锁协调多实例互斥
    │       → 返回 WarmUpResult { successCount=3, durationMs=85 }
    │
    └─(4)─ 监控验证
            → Micrometer: cache.warmup.total{cache="user_cache",status="success"} = 3
            → 日志: [WarmUp] cache=user_cache success=3/3 duration=85ms
```

---

## 8. 分布式协调

多实例部署下通过 Redisson 分布式锁协调，按缓存名粒度加锁：

```
cache:warmup:lock:user_cache
cache:warmup:lock:order_cache
```

- 获取锁成功 → 提交所有 key 到虚拟线程并发执行
- 锁在 `CompletableFuture.whenComplete()` 回调中释放（所有 key 完成后解锁）
- 获取锁失败 → 记录指标 `cache.warmup.lock.skipped`
- 无 Redisson 时自动降级为单机模式

---

## 9. 监控指标（Micrometer）

| 指标名                          | 类型      | Tag                              | 含义             |
|------------------------------|---------|----------------------------------|----------------|
| `cache.warmup.total`         | Counter | cache + status (success/failure) | 预热成功/失败条目数     |
| `cache.warmup.duration`      | Timer   | cache                            | 每次预热耗时         |
| `cache.warmup.lock.acquired` | Counter | cache                            | 锁获取成功次数        |
| `cache.warmup.lock.skipped`  | Counter | cache                            | 锁跳过次数（其他节点执行中） |

日志摘要示例：

```
[WarmUp] ==== Cache Warm-Up Summary ====
[WarmUp] cache=user_cache success=3/3 duration=1250ms
[WarmUp] cache=order_cache success=5/5 duration=320ms
[WarmUp] ==== Total: 2 tasks, 8 total, 8 success, 1570ms ====
```

---

## 10. 模块结构

```
architect-cache-warmup/src/main/java/com/cloud/arch/cache/warmup/
├── core/
│   ├── WarmUpTask.java               # 任务 POJO（cacheName, method, bean, remark）
│   ├── WarmUpResult.java             # 结果 POJO（totalCount/successCount/duration）
│   ├── WarmUpMeta.java               # 元数据视图（缓存级别，methods + sampleArgs）
│   ├── MethodMeta.java               # 方法元数据（beanName + methodName + paramTypes）
│   ├── WarmUpArgsProvider.java       # 参数来源接口（@FunctionalInterface）
│   ├── WarmUpRegistry.java           # 注册表（按 cacheName 索引，元数据汇编）
│   ├── WarmUpScanner.java            # 扫描器（SmartInitializingSingleton）
│   ├── WarmUpExecutor.java           # 执行器（虚拟线程并发 + CompletableFuture）
│   ├── WarmUpCoordinator.java        # 分布式协调器（Redisson 锁）
│   └── WarmUpTemplate.java           # 手动预热 API（返回 CompletableFuture）
├── config/
│   ├── WarmUpProperties.java         # 配置属性
│   ├── ConfigWarmUpArgsProvider.java  # 默认实现（YAML → args）
│   └── WarmUpAutoConfiguration.java  # 自动装配
├── endpoint/
│   └── WarmUpEndpoint.java          # REST 端点（POST 预热 + GET 元数据）
└── support/
    └── WarmUpMetrics.java           # Micrometer 指标
```

### Maven 依赖

```xml

<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-cache-warmup</artifactId>
</dependency>
```

依赖关系：

- `architect-cache-support`（provided）
- `architect-webmvc-boot-starter`
- `architect-redisson`（optional，分布式锁）
- `micrometer-core`（指标采集）
- **JDK 21+**（虚拟线程）

