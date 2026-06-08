# architect-cache vs 主流缓存方案对比

## 1. 架构定位

architect-cache 的本质是 **L1 Caffeine + L2 Redisson 双层缓存 + AOP 注解驱动 + 热点探测（可插拔）+ 预热 + 延迟双删**，对标
Spring Cache 生态里「开箱即用不够、需要自己搭」的中间地带。

对比对象：

- **Spring Cache**：标准注解缓存抽象，生态最广
- **JetCache**：阿里开源，国内流行的 L1/L2 方案
- **Layering Cache**：另一款 L1/L2 缓存框架
- **Ehcache**：老牌本地缓存，支持堆外内存和磁盘持久化
- **Hazelcast**：分布式内存数据网格，去中心化 P2P 架构
- **Caffeine 裸用**：当前性能最强的本地缓存库
- **Redisson 裸用**：Redis Java 客户端，功能最丰富的分布式缓存客户端

---

## 2. 性能对比

### 2.1 读操作延迟量级

| 方案 | L1 命中 | L2 命中 | L2 未命中 | AOP 开销 | 备注 |
|------|---------|---------|-----------|----------|------|
| architect-cache | <0.1ms | 1-5ms | DB 耗时 | ~0.01ms | Caffeine 存对象引用，零反序列化 |
| Spring Cache + Caffeine | <0.1ms | — | DB 耗时 | ~0.01ms | 无 L2，需手工追加 |
| Spring Cache + Redis | — | 1-5ms | DB 耗时 | ~0.01ms | 无 L1 加速 |
| JetCache | <0.1ms | 1-5ms | DB 耗时 | ~0.01ms | L1 存序列化字节，有反序列化开销 |
| Caffeine 裸用 | <0.1ms | — | — | 无 | 单机，无分布式能力 |
| Redisson 裸用 | — | 1-5ms | DB 耗时 | 无 | 无本地加速，无防击穿 |
| Ehcache（堆内） | <0.1ms | — | — | 无 | 纯本地，不适用分布式 |
| Ehcache（堆外） | ~0.5ms | — | — | 无 | 堆外内存绕过 GC，适合大 value |
| Hazelcast（Near Cache） | <0.1ms | ~0.5ms | DB 耗时 | 无 | 分布式内存网格，P2P 通信 |

**关键差异**：

- architect-cache 的 L1 命中路径不经过 Redisson，直接从 Caffeine 返回对象引用，是所有双层方案中 L1 路径最短的。
- JetCache L1 存的是序列化后的字节而非对象引用，每次 L1 命中都需反序列化。
- 热点探测上线后，热点 key 自动提升到本地 Caffeine，原走 L2 的 key 变成 L1 命中，效果接近自动预热。

### 2.2 写操作延迟

| 方案 | L1 写入 | L2 写入 | 集群一致性 | 总延迟 |
|------|---------|---------|-----------|--------|
| architect-cache | Caffeine.put（直接） | Redisson RMapCache.put | Pub/Sub 异步广播 | ~2-6ms |
| JetCache | Caffeine.put | Lettuce SETEX | Pub/Sub 异步广播 | ~2-6ms |
| Spring Cache + Redis | — | RedisTemplate.put | 无（无 L1） | ~1-3ms |
| Hazelcast | 写入本地分区 | P2P 备份到其他节点 | 内置 Raft | ~1-3ms |

### 2.3 并发场景性能特征

**缓存击穿对比**（100 并发同时 miss 同一 key，3 实例集群）：

| 方案 | DB 查询次数 | 机制 |
|------|-----------|------|
| architect-cache | **1 次** | 本地 synchronized + Redisson 分布式锁 + 三次检查 |
| Spring Cache sync=true | 3 次（每实例 1 次） | 仅本地锁 |
| JetCache | 1 次 | 分布式锁（纯 Redis 锁，无本地锁层） |
| Layering Cache | 3 次 | 仅本地锁 |
| 无防护 | 100 次 | — |

architect-cache 的两级锁设计：JVM 内各线程被 synchronized 拦住，只留 1 个与分布式锁交互，减少了 Redis 锁争抢的网络开销。

JetCache 仅有分布式锁，同 JVM 内不同线程也要争 Redis 锁，每个线程多出一次网络往返。

### 2.4 AOP 开销分析

所有注解驱动方案都有 AOP 开销（~0.01-0.05ms），在 DB 查询通常 10-100ms 的背景下可忽略。如果追求极致性能，应去掉注解直接用 Caffeine + Redisson 手写——但会丢失集群一致性、监控、防击穿等工程化能力。

---

## 3. 功能对比

### 3.1 全景功能矩阵

| 功能 | architect-cache | Spring Cache | JetCache | Layering Cache | Ehcache | Hazelcast | Caffeine 裸用 | Redisson 裸用 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| L1/L2 双层缓存 | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| Write-through 策略 | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| 集群 L1 一致性 | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| 防缓存击穿（分布式） | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ |
| 防缓存穿透（空值编码） | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ |
| 防缓存雪崩（随机 TTL） | ✅ | ❌ | ✅ | 依赖 Caffeine | ❌ | ❌ | ❌ | ❌ |
| TTL 异步续期 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 延迟双删 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 热点 Key 自动探测 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 缓存预热（分布式协调） | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| L1 运行时动态启停 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 启动类型安全校验 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Micrometer 监控 | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ |
| SpEL 表达式 | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Optional 透明包装 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 堆外内存 | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 磁盘持久化 | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 分布式锁（通用） | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| JCache（JSR-107） | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ |

### 3.2 独家能力详解

#### 热点探测（architect-cache 独有）

```
应用实例（N 个） ──TurnKeyCollector（双缓冲轮转）──> Netty Worker 集群 ──阈值判断──> Etcd
                                                          │
                                                   识别为热点后推送回客户端
                                                          │
应用实例 ──ReceiveNewKeySubscriber── 自动将该 key 提升到本地 Caffeine 加速
```

- 通过 TurnKeyCollector（双缓冲轮转桶算法）在客户端侧收集 key 访问频率
- WorkerScheduledPusher 定时（默认 500ms）将数据通过 Netty 发送到 Worker 集群
- Worker 集群按 key 哈希取模路由，确保同一 key 始终发到同一 Worker 节点
- Worker 根据 Etcd 中配置的 KeyRule（interval / threshold / duration）判定热点
- 热点判定后将 key 推回应用实例，自动加载到本地 Caffeine
- 其他方案要么完全不知道热点，要么靠人工分析慢查询日志。关闭 Etcd 配置后完全不影响其他功能，零侵入

#### TTL 异步续期（architect-cache 独有）

- 每次 `doGet()` 命中后，检查 Redis TTL 是否 < `preloadTime`（默认 300 秒）
- 满足条件则通过 Redisson 分布式锁（20ms 超时）续期到完整 TTL
- 每个 key 在 `refreshTimeCache`（ConcurrentMap）中限速，默认 30 秒内不重复刷新
- 热点 key 不会在过期边界经历击穿回源毛刺，其他方案对此无解

#### 延迟双删（architect-cache 独有）

```
事务提交 → 立即删除缓存 → DelayQueue 等待 N ms → 再次删除缓存
```

- `@TransactionalEventListener` + `DelayQueue`，事务提交后触发
- `delayEvictInterval` 默认 500ms，解决数据库读写分离场景的主从延迟问题
- 两次删除之间，从库可能尚未同步数据，导致缓存被填充为旧值；延迟二次删除消除这个窗口

#### 启动类型安全校验（architect-cache 独有）

- `@CacheResult` 同名缓存但不同返回类型的冲突，在启动阶段通过 `AbsCacheOperation.returnType` 校验
- 冲突时直接报错，防止运行时 `ClassCastException`
- 同类方案无一做到，这个问题通常在线上才暴露

#### 缓存预热（architect-cache 独有）

- `@CacheResult(warmup=true, remark="描述")` 标记可预热方法
- `WarmUpScanner` 启动时扫描所有 Bean，注册预热任务
- `WarmUpExecutor` 使用 JDK 21 虚拟线程并发执行预热
- `WarmUpCoordinator` 使用 Redisson 分布式锁协调多实例（只让一个节点执行预热）
- REST 端点：`GET /actuator/warmup/caches` 查看元数据，`POST /actuator/warmup/cache/{name}` 触发预热
- 完整链路：元数据视图 → 手动执行 → 结果反馈，不是简单的「启动时调几个方法」

#### L1 运行时动态启停（architect-cache 独有）

- `CacheManager.activateLocal(name)` / `detachLocal(name)` 运行时挂载/卸载 Caffeine 本地缓存
- 无需重启应用，可通过 REST 端点或管理控制台动态控制每个缓存实例的 L1 行为
- 适合灰度场景：先在部分节点关闭 L1 观察，确认无问题后再全面操作

### 3.3 其他功能对比要点

| 维度 | architect-cache | Spring Cache | JetCache | Layering Cache |
|------|:---:|:---:|:---:|:---:|
| L1/L2 双层缓存 | 原生 | 无（需手动整合） | 原生 | 原生 |
| 写入策略 | Write-through | Cache-aside | Write-through | Write-through |
| 集群 L1 一致性 | Pub/Sub + 本节点直接更新 + 发送方去重 | 无 L1 | 广播失效 | 广播失效 |
| 防缓存击穿 | 两级锁（synchronized + Redisson + 三次检查） | `@Cacheable(sync=true)` 仅本地锁 | 分布式锁 | 仅本地锁 |
| 防缓存雪崩 | `expire + random(randomBound)` | 需手动配置 | 支持 | 依赖 Caffeine |
| 防缓存穿透 | `NullValue` 哨兵 + 独立压缩 TTL | 需自行处理 | 支持 | 需自行处理 |
| TTL 异步续期 | `preloadTime` + 分布式锁 + key 级限流 | 无 | 无 | 无 |
| 延迟双删 | `@TransactionalEventListener` + `DelayQueue` | 无 | 无 | 无 |
| Micrometer 监控 | 4 个 Counter/Timer + 5 个派生 Gauge | 依赖外部 | 有 | 无 |

### 3.4 核心架构差异

**architect-cache**：
```
用户调用 → AOP 拦截 → SpEL 解析 key → CacheManager.get(cacheName)
    → RedisRemoteCache.get(key)
        → if L1 activated: AbstractLocalCache.get(key)
            → Caffeine 命中 → 返回
            → Caffeine 未命中 → synchronized(KEY_LOCKS) → 双检 → RedisRemoteCache.doGet(key)
                → RMapCache 命中 → write L1 → 返回
                → 未命中 → loadAndPut（Redisson 分布式锁 + 重试）→ write L1 + L2 → 返回
        → if L1 not activated: RedisRemoteCache.doGet(key)
```

**Spring Cache**：
```
用户调用 → AOP 拦截 → CacheManager.get(cacheName) → Cache.get(key)
    → 开发者自行实现全部逻辑（L1/L2/击穿/穿透/雪崩/一致性/监控）
```

**JetCache**：
```
用户调用 → AOP 拦截 → CacheManager.get(cacheName) → Cache.get(key)
    → Caffeine 命中（反序列化）→ 返回
    → Caffeine 未命中 → Redis 分布式锁 → Redis GET → 反序列化 → write Caffeine → 返回
```

---

## 4. 运维对比

### 4.1 部署复杂度

| 方案 | 最少依赖 | 完整功能依赖 | 部署难度 |
|------|---------|-------------|---------|
| architect-cache（基础模式） | Redis | — | 低 |
| architect-cache（+ 热点探测） | Redis | Etcd + Netty Worker 集群 | **高** |
| architect-cache（+ 预热） | Redis | — | 低 |
| Spring Cache + Caffeine | 无 | — | 极低 |
| Spring Cache + Redis | Redis | — | 低 |
| JetCache | Redis | — | 低 |
| Hazlecast | 无 | — | 极低（内嵌式） |
| Ehcache | 无 | — | 极低 |

### 4.2 可观测性

| 方案 | 指标数量 | 指标详情 | Grafana 集成 | 管理端点 |
|------|---------|---------|-------------|---------|
| architect-cache | 9 个 | gets(hit/hitLocal/miss) / loads(success/failure) / evictions + 5 个 Gauge(hitRate/hitL1Rate/missRate/loadAvgTime/loadFailRate/size) | Micrometer → Prometheus 直出 | `/actuator/warmup/**` |
| Spring Cache | 0 | 需外挂 | 需自建 | 无 |
| JetCache | ~6 个 | hit/miss/load/loadTime 等 | Micrometer → Prometheus | 无 |
| Hazelcast | ~20 个 | 丰富的内存/分区/操作指标 | JMX → JMX Exporter | 内置 Management Center |
| Ehcache | ~10 个 | 命中/未命中/驱逐等 | JMX / 自定义 | 无 |

architect-cache 的派生 Gauge 直接出 `hit.rate` / `miss.rate`，不需要在 Prometheus 侧再写 `rate()` 表达式，降低了看板搭建门槛。

### 4.3 运行时管控能力

| 能力 | architect-cache | 其他方案 |
|------|:---:|:---:|
| 运行时开关 L1 | `activateLocal` / `detachLocal` | 无 |
| 手动触发预热 | `POST /actuator/warmup/cache/{name}` | 无 |
| 查看预热元数据 | `GET /actuator/warmup/caches` | 无 |
| 动态调整 TTL | 需改配置重启 | 大部分同 |
| 按 key 删除缓存 | `@CacheEvict` 注解触发 | Spring Cache 有 `@CacheEvict` |

### 4.4 升级与兼容性风险

| 方案 | 生态风险 |
|------|---------|
| architect-cache | 内部方案，依赖自维护。Redisson/Caffeine 底层升级需自行回归。不兼容 Spring Cache 注解，与第三方库共存有摩擦 |
| JetCache | 阿里维护，社区活跃，版本迭代稳定。但强依赖 Lettuce，换 Redisson 成本高 |
| Spring Cache | Spring 官方，最稳定。但功能过少，实际项目中易退化为「注解 + 手写」混合 |
| Caffeine | Ben Manes 单人维护，质量极高且被广泛使用。但它只是本地缓存，不解决分布式问题 |
| Hazelcast | 成熟商业产品。嵌入式模式有脑裂风险，Client/Server 模式运维成本高 |
| Ehcache | Terracotta 维护。分布式方案（Terracotta Server Array）已事实上停止维护 |

---

## 5. 优劣势分析

### 5.1 优势

**工程化程度高，开箱覆盖缓存三大难题**

防击穿、防穿透、防雪崩的策略直接做到 `doGet`/`doPut` 实现里，开发者不用关心。Spring Cache 只定义抽象（CacheManager/Cache），安全策略全靠使用者自己实现。

**集群 L1 一致性方案务实**

很多 L1/L2 方案直接跳过集群一致性问题。本方案用 Pub/Sub 广播 + 本节点直接更新（非淘汰）+ 发送方去重（`CacheNodePolicy`），避免了「所有节点全部 evict 重新读 L2」的缓存风暴。

**热点探测是差异化杀手锏（可插拔）**

唯一一个把热点探测作为一等公民的缓存方案。其他方案最多建议用 Caffeine 加速，但不会自动告诉你哪个 key 热、不会自动联动 L1 加速。关闭 Etcd 配置后完全不影响其他功能，零侵入。

**启动时类型校验防线上事故**

`@CacheResult` 同名不同返回类型是极易犯的错误，运行时爆发就是 `ClassCastException`。校验直接把问题挡在启动阶段，同类方案无一做到。

**预热设计深思熟虑**

虚拟线程并发 + 分布式锁互斥 + 元数据视图到手动执行的完整链路（`GET /actuator/warmup/caches` → `POST /actuator/warmup/cache/{name}`），不是简单的「启动时调几个方法」。

**内置 Micrometer 指标**

开箱提供 4 个基础指标（命中/加载/淘汰/大小）+ 5 个派生 Gauge（命中率/L1 命中率/未命中率/平均加载耗时/加载失败率），Prometheus/Grafana 可直接消费，无需手动计算比率。

### 5.2 劣势

**与 Spring Cache 生态不兼容**

`@CacheResult/@CachePut/@CacheEvict` 是自定义注解，不能和 Spring Cache 的 `@Cacheable/@CachePut/@CacheEvict` 互通。如果依赖了使用 Spring Cache 的第三方库，两套注解共存会造成混淆。

**热点探测模块有独立运维成本**

启用热点探测需要部署 Etcd + Netty Worker 集群，适合有运维平台支撑的团队。不过该模块可插拔，不启用时无额外依赖。

**文档和社区**

Spring Cache 有官方文档 + 海量社区。JetCache 有阿里背书 + 活跃 GitHub。本方案是内部方案，新人接入主要靠 README 和设计文档。

**缺少堆外内存和磁盘持久化**

Ehcache 支持堆外内存（减少 GC 压力）和磁盘持久化（重启不丢缓存），architect-cache 只依赖 Caffeine 堆内内存 + Redis。

**不支持 JCache（JSR-107）**

Spring Cache 和 Ehcache 通过适配器支持 JCache 标准注解，与 Java EE / Jakarta EE 生态更兼容。

---

## 6. 场景推荐

| 场景 | 推荐方案 | 理由 |
|------|---------|------|
| 简单缓存，单体应用 | Spring Cache + Caffeine | 零依赖，5 分钟接入 |
| 有 Redis，不想自己搭 | JetCache | 阿里生态首选，社区活跃 |
| 需要热点探测 + 预热 + 运维管控 | **architect-cache** | 唯一具备这三项能力的方案 |
| 去中心化，不要 Redis | Hazelcast | 内嵌分布式内存网格 |
| 大 value / 堆外内存 / 持久化 | Ehcache | 堆外绕过 GC，磁盘持久化 |
| 极致性能 | Caffeine + Redisson 手写 | 零 AOP 开销，但需接受缺少工程化能力 |
| 仅本地缓存 | Caffeine | 性能最强，API 最优雅 |
| 仅分布式缓存 | Redisson | 功能最丰富的 Redis 客户端 |

---

## 7. 总结

architect-cache 不是在重复造轮子，而是在 Spring Cache 的「够用」和自建运维体系的「太贵」之间，提供了一个工程化程度很高的中间方案。

核心护城河：**热点探测（可插拔）+ 预热（分布式协调）+ 启动类型安全校验**，这三项能力在同类方案中独有。代价是热点探测模块的独立运维成本和与 Spring Cache 注解生态的不兼容。
