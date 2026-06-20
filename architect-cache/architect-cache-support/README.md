# architect-cache-support 二级缓存组件使用文档

## 1. 概述

本组件提供基于 **Caffeine (L1 本地) + Redis (L2 远程)** 的二级缓存解决方案，通过注解驱动的方式为 Spring Boot 应用提供方法级缓存能力。

### 核心特性

- **L1+L2 两级缓存**：Caffeine 本地缓存（近端低延迟） + Redis 远程缓存（跨节点共享）
- **L1 动态激活/卸载**：运行时可按缓存实例动态开关本地缓存，无需重启
- **三层防穿透保护**：锁内双检防击穿、随机过期防雪崩、空值缓存防穿透
- **集群一致性**：Redis Pub/Sub 广播缓存失效事件，保证多节点 L1 数据一致
- **热点保护**：TTL 异步刷新机制，热点 key 过期前自动续期
- **Micrometer 集成**：内置缓存命中率、加载耗时等指标统计

### 架构简图

```
@CacheResult / @CachePut / @CacheEvict (注解层)
                ↓
    AbstractRemoteCache (L2 Redis)
         ↕ 激活时代理
    AbstractLocalCache (L1 Caffeine)
         ↓
    RefreshPolicy (Redis Pub/Sub 集群失效广播)
```

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-cache-support</artifactId>
</dependency>
```

### 2.2 启用缓存

在配置类添加 `@EnableCaching` 并注入 `RedisCacheManager`：

```java
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public RedisCacheManager cacheManager(RedissonClient redissonClient) {
        return new RedisCacheManager(
            StatsManager.disabledManager(),
            redissonClient,
            new RemoteCacheTtlRefresher()
        );
    }
}
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

## 3. 注解说明

### 3.1 @CacheResult — 查询缓存

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheResult {

    /** 缓存实例名称 */
    String[] names() default "";

    /** 缓存 key，支持 SpEL 表达式 */
    String key() default "";

    /** key 生成器 Bean 名称（与 key 互斥） */
    String keyGenerator() default "";

    /** SpEL 条件表达式，满足条件才缓存 */
    String condition() default "";

    /** SpEL 排除表达式，满足条件不缓存 */
    String unless() default "";

    /** 是否启用 L1 本地缓存 */
    boolean enableLocal() default false;

    /** L1 缓存配置 */
    Local local() default @Local();

    /** L2 缓存配置 */
    Remote remote() default @Remote();
}
```

#### 使用示例

```java
// 基础用法：缓存用户信息，key 为 userId
@CacheResult(name = "user", key = "#userId")
public User getUser(Long userId) { ... }

// 条件缓存：仅缓存非空结果
@CacheResult(name = "user", key = "#userId", unless = "#result == null")
public User getUser(Long userId) { ... }

// 开启 L1 本地缓存
@CacheResult(name = "hot:config", key = "#key",
             enableLocal = true,
             local = @Local(maximumSize = 500, expire = 300))
public Config getConfig(String key) { ... }

// 自定义 L2 过期时间和刷新策略
@CacheResult(name = "product", key = "#id",
             remote = @Remote(expire = 3600, randomBound = 600, enableRefresh = true))
public Product getProduct(Long id) { ... }
```

### 3.2 @CachePut — 更新缓存

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {

    String[] names() default "";
    String key() default "";
    String keyGenerator() default "";
    String condition() default "";
    String unless() default "";
    boolean enableLocal() default false;
    Local local() default @Local();
    Remote remote() default @Remote();
}
```

#### 使用示例

```java
// 方法执行后更新缓存，缓存值为方法返回值
@CachePut(name = "user", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}

// 条件更新：仅更新 VIP 用户缓存
@CachePut(name = "user", key = "#user.id", condition = "#user.vip")
public User updateUser(User user) { ... }
```

### 3.3 @CacheEvict — 删除缓存

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

    String[] names() default "";
    String key() default "";
    String keyGenerator() default "";
    String condition() default "";

    /** 是否清除该缓存实例下的全部数据 */
    boolean allEntries() default false;

    /** 是否在方法执行前删除缓存（默认方法执行后） */
    boolean beforeInvocation() default false;
}
```

#### 使用示例

```java
// 删除单个缓存
@CacheEvict(name = "user", key = "#userId")
public void deleteUser(Long userId) { ... }

// 清除全部缓存
@CacheEvict(name = "user", allEntries = true)
public void clearAllUsers() { ... }

// 方法执行前删除缓存（防止方法执行期间脏读）
@CacheEvict(name = "user", key = "#userId", beforeInvocation = true)
public void evictBeforeUpdate(Long userId) { ... }
```

### 3.4 @Local — L1 缓存配置

```java
public @interface Local {
    int initialSize()  default 512;     // 初始容量
    int maximumSize()  default 2048;    // 最大容量
    long expire()      default 600;     // 过期时间（秒）
    ExpireMode expireMode() default ExpireMode.WRITE;  // 过期模式
}
```

**过期模式**：

| 模式 | 含义 |
|------|------|
| `ExpireMode.WRITE` | 写入后开始计时，到期淘汰 |
| `ExpireMode.ACCESS` | 最后访问后开始计时，热点自动续期 |

### 3.5 @Remote — L2 缓存配置

```java
public @interface Remote {
    long expire()       default 1800;   // 固定过期时间（秒）
    int randomBound()   default 1200;   // 随机过期偏移上限（秒）
    int magnification() default 3;      // 空值过期时间除数
    long preloadTime()  default 300;    // 提前刷新时间（秒）
    boolean enableRefresh() default false; // 是否启用 TTL 刷新
}
```

**实际过期时间** = `expire` + `random(0, randomBound)`

- `expire = 1800, randomBound = 1200` → 实际过期 1800~3000 秒，避免缓存雪崩

**空值过期时间** = `(expire + random) / magnification`

- 空值缓存自动缩短，例如 `(1800+600)/3 = 800` 秒

---

## 4. 编程式 API

除了注解，也可以直接通过 `Cache` 或 `CacheManager` 编程使用：

```java
@Component
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public User getUser(Long userId) {
        // 获取或创建缓存实例
        Cache userCache = cacheManager.getAndAdd("user", 
            CacheSettings.builder()
                .expire(1800)
                .randomBound(1200)
                .allowNullValue(true)
                .magnification(3)
                .build());

        // 查询缓存
        User user = userCache.get(userId);
        if (user != null) {
            return user;
        }

        // 缓存未命中，从数据源加载
        user = userRepository.findById(userId);
        userCache.put(userId, user);
        return user;
    }
}
```

### Cache 接口

```java
public interface Cache {
    String getName();
    <T> T get(Object key);
    <T> T get(Object key, Class<T> type);
    <T> T get(Object key, Callable<T> valueLoader);
    void put(Object key, Object value);
    void evict(Object key);
    void clear();
    long cacheSize();
    Object putIfAbsent(Object key, Object value);
    void evictOrClear(Object key);
    boolean evictIfPresent(Object key);
    boolean invalidate();
}
```

---

## 5. L1/L2 动态切换

组件支持**运行时动态开启/关闭 L1 本地缓存**，无需重启应用：

```java
// 激活指定缓存实例的 L1
cacheManager.activateLocal("user");

// 卸载指定缓存实例的 L1
cacheManager.detachLocal("user");
```

典型场景：缓存预热期关闭 L1 让流量直接打到 Redis，稳定后启用 L1 降低延迟。

---

## 6. 集群一致性

### 6.1 工作原理

```
节点 A 更新缓存
  → RedisRemoteCache.doPut(key, value)     // L2 写入
  → CaffeineLocalCache.doPut(key, value)   // 本节点 L1 更新
  → RefreshPolicy.sendEvict(name, key)     // Pub/Sub 广播
      ↓
节点 B、C 收到 Redis 消息
  → CaffeineLocalCache.doEvict(key)        // 淘汰本地 L1
  → 下次 get 从 L2 重新加载
```

### 6.2 配置集群失效

```java
@Configuration
public class CacheClusterConfiguration {

    @Bean
    public RefreshPolicy refreshPolicy(RedissonClient redissonClient,
                                        CacheNodePolicy nodePolicy) {
        return new RedisTopicRefreshPolicy("cache:refresh:topic", redissonClient, nodePolicy);
    }

    @Bean
    public CacheEventListener cacheEventListener(RedisCacheManager cacheManager,
                                                  CacheNodePolicy nodePolicy) {
        return new RedisRefreshEventListener("cache:refresh:topic", cacheManager, nodePolicy);
    }

    @Bean
    public CacheNodePolicy nodePolicy() {
        return () -> ThreadLocalRandom.current().nextLong();
    }
}
```

### 6.3 单机模式

不需要集群失效时使用默认策略即可（消息不做任何事）：

```java
new DefaultRefreshPolicy()  // publish() 为空实现
```

---

## 7. 防穿透/击穿/雪崩

### 7.1 防缓存穿透（null 值查询）

开启后，null 值会被编码为 `NullValue` 实例缓存，避免恶意查询穿透到数据库：

```java
CacheSettings.builder()
    .allowNullValue(true)    // 开启空值缓存
    .magnification(3)        // 空值过期 = 正常过期 / 3
    .build();
```

空值缓存写入流程：

```
db 查询返回 null → toStoreValue(null) → NullValue.INSTANCE → 缓存到 Redis
下次查询 → L2 命中 NullValue → fromStoreValue() → 返回 null
```

### 7.2 防缓存击穿（热点 key 过期穿透）

L1 未命中时自动加锁，同一 key 只允许一个线程穿透到 L2/数据源：

```java
// AbstractLocalCache.get() 核心逻辑
synchronized (KEY_LOCKS.computeIfAbsent(key, v -> new Object())) {
    value = this.doGet(key);           // 双检
    if (value != null) return value;
    value = remoteCache.doGet(key);    // 只有一个线程执行到这里
    if (value != null) this.doPut(key, value);
    return value;
}
```

> 锁粒度是 key 级别，不同 key 间无竞争。击穿防护始终开启，无需配置。

### 7.3 防缓存雪崩（批量同时过期）

过期时间 = `expire` + `random(0, randomBound)`，同一批数据实际过期时间分散到长达 `randomBound` 的时间窗口内：

```java
// RedisRemoteCache.doPut()
ThreadLocalRandom random     = ThreadLocalRandom.current();
long              expireTime = settings.getExpire() + random.nextInt(settings.getRandomBound());
```

---

## 8. TTL 异步刷新

热点 key 在距离过期 `preloadTime` 秒时，异步延长其 Redis 过期时间：

```
                     preloadTime (300s)
    |<─────────────────────────────────────>|
    ↑                                     ↑
  现在                          距离过期还有300s
                                      ↓
                          触发异步刷新，续期到 expire
    
实际过期时间线：
    |---------- expire + random ----------|
    |                          ↓ 刷新
    |---------- expire + random ----------|
```

配置方式：

```java
@CacheResult(name = "hot:product", key = "#id",
    remote = @Remote(
        expire = 1800,
        randomBound = 600,
        enableRefresh = true,   // 开启
        preloadTime = 300       // 过期前 300 秒触发
    ))
public Product getProduct(Long id) { ... }
```

刷新有频率控制：同一 key 在 `DEFAULT_REFRESH_INTERVAL`（30秒）内不会重复触发刷新。

---

## 9. 缓存统计

### 9.1 开启 Micrometer 统计

```java
@Bean
public RedisCacheManager cacheManager(RedissonClient redissonClient,
                                       MeterRegistry meterRegistry) {
    CacheStatsManager statsManager = new CacheStatsManager(meterRegistry);
    return new RedisCacheManager(statsManager, redissonClient,
                                  new RemoteCacheTtlRefresher());
}
```

### 9.2 指标项

| 指标 | 说明 |
|------|------|
| `cache.hits` | L1 命中次数 |
| `cache.misses` | 未命中次数 |
| `cache.evictions` | 淘汰次数 |
| `cache.load.success` | 加载成功次数 |
| `cache.load.fail` | 加载失败次数 |
| `cache.load.time` | 加载耗时 |
| `cache.size` | 缓存条目数 |

---

## 10. 配置参考

### 10.1 注解参数默认值

| 参数 | 默认值 | 来源常量 |
|------|--------|---------|
| `@Local.initialSize` | 512 | `CacheConstants.INITIAL_SIZE` |
| `@Local.maximumSize` | 2048 | `CacheConstants.MAXIMUM_SIZE` |
| `@Local.expire` | 600 秒 | `CacheConstants.LOCAL_EXPIRE` |
| `@Local.expireMode` | `WRITE` | — |
| `@Remote.expire` | 1800 秒 | `CacheConstants.REMOTE_EXPIRE` |
| `@Remote.randomBound` | 1200 秒 | `CacheConstants.RANDOM_BOUND` |
| `@Remote.magnification` | 3 | `CacheConstants.MAGNIFICATION` |
| `@Remote.preloadTime` | 300 秒 | `CacheConstants.PRELOAD_TIME` |
| `@Remote.enableRefresh` | false | — |

### 10.2 TTL 刷新线程池参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 核心线程数 | `CPU核数` | 由 `Runtime.availableProcessors()` 决定 |
| 最大线程数 | `CPU核数 × 2 + 1` | — |
| 队列容量 | 256 | 超出后触发 `DiscardOldestPolicy` |
| 线程存活时间 | 120 秒 | — |

---

## 11. 最佳实践

### 11.1 哪些场景开启 L1

```
✅ 适合 L1：
  - 读多写少的热点数据（配置字典、商品详情）
  - 对延迟敏感（<1ms 要求）
  - 批量查询后的单条回查

❌ 不适合 L1：
  - 写频率高的数据（容易频繁失效）
  - 数据量极大（可能撑爆本地内存）
  - 对一致性要求极高（发布订阅有延迟）
```

### 11.2 L1 容量规划

```
L1.maximumSize = 热点key数量 × 2

例如：100 个高频商品，每个缓存 2KB
  → maximumSize = 200
  → 内存占用 ≈ 200 × 2KB = 400KB（可忽略）
```

### 11.3 过期时间建议

| 数据特征 | `expire` | `randomBound` | `enableRefresh` |
|---------|---------|---------------|-----------------|
| 可变配置 | 600~1800 | 300~600 | false |
| 热点数据 | 1800~7200 | 600~1200 | **true** |
| 静态字典 | 7200+ | 1200+ | false |
| 用户 Session | 600~1800 | 300 | false |

### 11.4 异常处理

写操作 L2 失败时的降级策略（P3 修复后）：

- L2 写入成功 → 本节点 L1 直接更新（零穿透）
- L2 写入失败 → 本节点 L1 淘汰（防脏读）
- 无论成败 → 其他节点 L1 通过 Pub/Sub 淘汰

```java
// 调用方无需感知缓存异常，组件内部已 try-catch
userCache.put("key", value);  // L2 失败只记 warn 日志，不抛异常
```

### 11.5 缓存预热

应用重启或定时刷新时，可配合 `architect-cache-warmup` 模块实现自动/手动缓存预热，消除冷启动延迟。详见 [architect-cache-boot-starter 使用文档](../architect-cache-boot-starter/README.md#10-缓存预热)。

### 11.6 缓存 Key 设计

```
✅ 推荐：业务前缀:实体名:业务ID
  user:1001, product:50023, config:site_settings

❌ 避免：
  - 纯数字 ID（可能冲突）
  - 含特殊字符未经编码
  - 过长的 key（影响 Redis 性能）
```

---

## 12. 常见问题

**Q: 何时用注解，何时用编程式 API？**

- 注解适合 95% 的场景：方法返回值缓存、标准 CRUD
- 编程式 API 适合：动态 key 列表、条件复杂的缓存逻辑、非方法边界缓存

**Q: L1 开启后节点间数据不一致怎么办？**

Redis Pub/Sub 消息延迟通常在 10ms 以内。如果对一致性要求极高，可以：
1. 缩短 `@Local.expire` 值（如 30 秒），让 L1 更快自然过期
2. 写操作后手动调 `cacheManager.detachLocal()` 临时关闭 L1 再重新激活

**Q: `expire` 和 `randomBound` 怎么配合？**

实际 TTL = `expire` + `random(0, randomBound)`。如果希望 30~40 分钟过期：
- `expire = 1800`（30 分钟），`randomBound = 600`（0~10 分钟）

**Q: 为什么空值也要缓存？**

防止缓存穿透。恶意查询不存在的 key 时，如果不缓存空值，每次都穿透到数据库。空值缓存时间更短（`/magnification`），在保护数据库和快速生效之间平衡。

**Q: 是否支持批量操作？**

当前版本仅支持单个 key 操作，不支持 `getAll`/`putAll`。如需批量操作，建议在业务层循环调用或使用 Redis 管道。

**Q: 可以只用 L2 不用 L1 吗？**

可以。不设置 `enableLocal = true` 即为纯 Redis 缓存模式。
