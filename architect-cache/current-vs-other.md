# architect-cache vs 主流缓存方案对比

## 1. 架构定位

architect-cache 的本质是 **L1 Caffeine + L2 Redisson 双层缓存 + AOP 注解驱动 + 热点探测（可插拔）+ 预热 + 延迟双删**，对标
Spring Cache 生态里「开箱即用不够、需要自己搭」的中间地带。

对比对象：

- **Spring Cache**：标准注解缓存抽象，生态最广
- **JetCache**：阿里开源，国内流行的 L1/L2 方案
- **Layering Cache**：另一款 L1/L2 缓存框架

---

## 2. 核心能力对比

| 维度            |                 architect-cache                  |         Spring Cache         |   JetCache    | Layering Cache |
|---------------|:------------------------------------------------:|:----------------------------:|:-------------:|:--------------:|
| L1/L2 双层缓存    |                        原生                        |           无（需手动整合）           |      原生       |       原生       |
| L1 运行时启停      |          支持 `activateLocal/detachLocal`          |              无               |       无       |       无        |
| 写入策略          |                  Write-through                   |         Cache-aside          | Write-through | Write-through  |
| 集群 L1 一致性     |           Pub/Sub 广播 + 本节点直接更新 + 发送方去重           |             无 L1             |     广播失效      |      广播失效      |
| 防缓存击穿         |   两级锁（本地 synchronized + Redisson 分布式锁 + 三次检查）    | `@Cacheable(sync=true)` 仅本地锁 |     分布式锁      |      仅本地锁      |
| 防缓存雪崩         |    内置 TTL 随机抖动 `expire + random(randomBound)`    |            需手动配置             |      支持       |  依赖 Caffeine   |
| 防缓存穿透         |          空值缓存 `NullValue` 哨兵 + 独立压缩 TTL          |            需自行处理             |      支持       |     需自行处理      |
| TTL 异步续期      |        支持（`preloadTime` + 分布式锁 + key 级限流）        |              无               |       无       |       无        |
| 延迟双删          | 支持（`@TransactionalEventListener` + `DelayQueue`） |              无               |       无       |       无        |
| 热点探测          |  有（可插拔，Etcd + Netty Worker + TurnKeyCollector）   |              无               |       无       |       无        |
| 缓存预热          |           有（虚拟线程并发 + 分布式锁互斥 + REST 端点）           |              无               |       无       |       无        |
| 同名缓存类型校验      |                   启动时校验，冲突直接报错                   |              无               |       无       |       无        |
| Micrometer 监控 |        有（4 个 Counter/Timer + 5 个派生 Gauge）        |           无（依赖外部）            |       有       |       无        |
| SpEL 表达式      |             key / condition / unless             |              支持              |      支持       |      不支持       |
| Optional 透明包装 |                     自动拆包/重包                      |             不透明              |      不透明      |      不透明       |

---

## 3. 优劣势分析

### 3.1 优势

**工程化程度高，开箱覆盖缓存三大难题**

防击穿、防穿透、防雪崩的策略直接做到 `doGet`/`doPut` 实现里，开发者不用关心。Spring Cache
只定义抽象（CacheManager/Cache），安全策略全靠使用者自己实现。

**集群 L1 一致性方案务实**

很多 L1/L2 方案直接跳过集群一致性问题。本方案用 Pub/Sub 广播 + 本节点直接更新（非淘汰）+ 发送方去重（`CacheNodePolicy`
），避免了「所有节点全部 evict 重新读 L2」的缓存风暴。

**热点探测是差异化杀手锏（可插拔）**

唯一一个把热点探测作为一等公民的缓存方案。其他方案最多建议用 Caffeine 加速，但不会自动告诉你哪个 key 热、不会自动联动 L1
加速。关闭 Etcd 配置后完全不影响其他功能，零侵入。

**启动时类型校验防线上事故**

`@CacheResult` 同名不同返回类型是极易犯的错误，运行时爆发就是 `ClassCastException`。校验直接把问题挡在启动阶段，同类方案无一做到。

**预热设计深思熟虑**

虚拟线程并发 + 分布式锁互斥 + 元数据视图到手动执行的完整链路（`GET /actuator/warmup/caches` →
`POST /actuator/warmup/cache/{name}`），不是简单的「启动时调几个方法」。

**内置 Micrometer 指标**

开箱提供 4 个基础指标（命中/加载/淘汰/大小）+ 5 个派生 Gauge（命中率/L1 命中率/未命中率/平均加载耗时/加载失败率），Prometheus/Grafana
可直接消费，无需手动计算比率。

### 3.2 劣势

**与 Spring Cache 生态不兼容**

`@CacheResult/@CachePut/@CacheEvict` 是自定义注解，不能和 Spring Cache 的 `@Cacheable/@CachePut/@CacheEvict` 互通。如果依赖了使用
Spring Cache 的第三方库，两套注解共存会造成混淆。

**热点探测模块有独立运维成本**

启用热点探测需要部署 Etcd + Netty Worker 集群，适合有运维平台支撑的团队。不过该模块可插拔，不启用时无额外依赖。

**文档和社区**

Spring Cache 有官方文档 + 海量社区。JetCache 有阿里背书 + 活跃 GitHub。本方案是内部方案，新人接入主要靠 README 和设计文档。

---

## 4. 场景推荐

| 场景                 | 推荐方案                              |
|--------------------|-----------------------------------|
| 简单缓存，单体应用          | Spring Cache + Caffeine           |
| 需要 L1/L2，不想自己搭     | JetCache                          |
| 需要热点探测 + 预热 + 运维管控 | **architect-cache**               |
| 超高并发，极致性能          | Caffeine + Redisson 手写（去掉 AOP 开销） |

---

## 5. 总结

architect-cache 不是在重复造轮子，而是在 Spring Cache 的「够用」和自建运维体系的「太贵」之间，提供了一个工程化程度很高的中间方案。

核心护城河：**热点探测（可插拔）+ 预热（分布式协调）+ 启动类型安全校验**，这三项能力在同类方案中独有。
