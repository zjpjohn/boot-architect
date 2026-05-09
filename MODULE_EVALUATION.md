# boot-architect 中间件体系评价报告

## 概述

boot-architect 是一套基于 Spring Boot 深度封装的基础设施中间件体系，覆盖异步事务、领域事件、缓存、聚合模式、Web 增强、权限等核心场景。全部模块由同一工程师设计实现，经过 10 条业务线全量生产验证。

核心设计哲学：

```
注解标记（声明式）→ 自动装配（约定优于配置）→ 拦截/增强（AOP + TransactionSynchronization）→ 业务无感知
```

---

## 模块总览

| 模块 | 定位 | 核心注解 | 存储依赖 |
|------|------|---------|---------|
| architect-transaction | 异步事务日志 | `@TxAsync` | MySQL |
| architect-event | 领域事件 + MQ 投递 | `@EventSubscribeHandler` | MySQL + MQ |
| architect-cache | 二级缓存 | `@CacheResult` `@CachePut` `@CacheEvict` `@Local` `@Remote` | Caffeine + Redisson |
| architect-aggregate | DDD 聚合模式 | 无注解（编程式 API） | 无 |
| architect-webmvc-boot-starter | Web 层增强 | `@ApiBody` `@ApiVersion` | 无 |
| architect-webmvc-security | 方法级权限 | `@Permission` | 数据库 |

---

## 各模块详细评价

### 1. architect-transaction（异步事务日志）

**功能**：`@TxAsync` 注解将方法调用与当前事务绑定，事务提交后异步执行。失败自动指数退避重试，超最大次数标记死信。内置补偿扫描 + 僵死任务修复 + 分布式锁调度。

**优点**：

- 接入成本极低，一个注解替代手写 Job + 重试 + 补偿逻辑
- 四层防护（即时执行 → 延迟重试 → 补偿扫描 → 僵死修复），任务可靠性高
- 版本控制（`AsyncTxVersion`）是竞品中少有的能力，代码升级时自动跳过旧版本任务
- 分库分表原生支持（shardKey 贯穿所有 SQL）
- 业务线程池与重试线程池隔离，互不影响
- 不依赖外部 MQ，降低架构复杂度

**缺点**：

- 仅支持 JDBC 存储，缺少 Redis/文件等存储扩展
- 无 Dashboard 监控界面（任务堆积量、成功率、重试趋势）
- 死信任务只能通过 SQL 手动重跑，无 API 重新投递
- 参数序列化强依赖 fastjson2，泛型集合参数（`List<User>`）反序列化可能丢类型信息
- 补偿任务拒绝策略为 `DiscardPolicy`，极端高并发下可能丢任务

---

### 2. architect-event（领域事件 + 消息队列）

**功能**：领域对象在业务方法内发布事件（`DomainEventPublisher.publish()`），事务提交后自动落库并投递 MQ。消费端通过 `@EventSubscribeHandler` 处理，内置 INSERT IGNORE + 唯一索引实现幂等消费。支持 Kafka、RabbitMQ、Pulsar、RocketMQ (两种版本) 五种队列。

**优点**：

- 事件发布对领域对象完全透明，领域对象不需要成为 Spring Bean
- 事务同步 + 本地事件表保证 at-least-once 投递
- 五种 MQ 统一抽象，切换只需改配置
- 幂等方案实用（INSERT IGNORE + InnoDB 唯一索引），不依赖 Redis
- 失败补偿表 + 定时任务独立处理，不阻塞正常消费
- SpEL 表达式缓存优化，避免重复编译

**缺点**：

- 幂等方案依赖 MySQL 唯一索引，非 MySQL 场景需额外适配
- 事件存储强依赖 JDBC（虽有 RocksDB 扩展但非默认）
- 缺少事件溯源/事件版本化能力
- 无死信队列（DLQ）概念，消费失败直接记录补偿表
- 无事件重放/回放能力

---

### 3. architect-cache（二级缓存）

**功能**：Caffeine（本地）+ Redisson（分布式）双层缓存，`@CacheResult` 读缓存、`@CachePut` 写缓存、`@CacheEvict` 清除缓存，`@Local`/`@Remote` 控制缓存层级。支持缓存穿透/击穿/雪崩防护，热 Key 自动检测并短路到本地缓存，延迟双删保证一致性。

**优点**：

- 双层架构兼顾性能（本地）和一致性（分布式）
- 热 Key 检测机制自动化，无需人工配置热点数据
- 缓存双删 + MQ 广播解决分布式缓存一致性问题
- 支持多种缓存策略（读穿/写穿/写回）

**缺点**：

- 缓存更新依赖 MQ 广播，增加 MQ 依赖
- 热 Key 检测阈值需根据业务调优，默认值不一定适合所有场景
- 缺少缓存预热机制
- 缓存监控指标不足（命中率、加载耗时等）

---

### 4. architect-aggregate（DDD 聚合模式）

**功能**：聚合根构造时自动深拷贝快照，`changed()` 方法通过反射遍历字段比对 root vs snapshot，返回仅含变更字段的新实例。仓储层可根据变更字段生成增量 UPDATE。支持实体集合 CRUD 变更追踪、分组变更扫描、链式 API。

**优点**：

- 快照比对模式实现最小化 SQL UPDATE，减少锁竞争和 binlog 量
- 链式 API（`peek` / `map` / `save`）支持流畅的 lambda 操作
- 实体集合完整 CRUD 追踪（new / changed / removed）
- `@Ignore(group)` 支持按业务场景差异化更新
- 线程安全设计（`Lazy<T>` DCL + volatile、`ForyDeepCopier` SingletonHolder）

**缺点**：

- 反射遍历字段有一定性能开销（但 `getDeepDeclaredFields` 已有 Class 级缓存）
- 仅支持浅层字段比对，嵌套对象的变更检测需依赖 `DeepEquals`
- 依赖第三方反射工具包（`reflection/` 包），升级维护需自行跟进
- `Collectors.toMap` 遇到重复 ID 会抛 `IllegalStateException`，无兜底

---

### 5. architect-webmvc-boot-starter（Web 层增强）

**功能**：Spring MVC 深度定制，包括异常统一处理（14 种异常分类）、响应体自动包装（`@ApiBody`）、响应 AES 加密（CBC/ECB）、参数类型转换（枚举 + 4 种日期）、API 版本路由（`@ApiVersion`）、字典端点暴露。

**优点**：

- `@ApiBody` 一个注解同时控制响应包装 + 加密，避免注解泛滥
- 异常处理优先级链（`WebmvcHandlerAdvice(HIGHEST)` → `GenericHandlerAdvice(LOWEST)`），特定异常优先、兜底不漏
- `WebMvcRegistrations` 直接替换 HandlerAdapter 来植入 Advice，无注册时序问题
- API 版本路由基于 Spring `RequestCondition`，与原 RequestMapping 体系深度融合
- 字典端点通过 `InitializingBean` + 反射动态注册，无需手写 Controller

**缺点**：

- 响应加密每次生成新密钥并通过 Header 传递，客户端需配合解析 Header
- 版本路由正则 `(\d\.)+\d` 使用 `find()` 匹配，可能匹配到 URL 中的非版本号
- 字典端点反射注册方式在 Spring Boot 4.0 中需验证兼容性
- 缺少请求日志/链路追踪内置支持

---

### 6. architect-webmvc-security（方法级权限）

**功能**：`@Permission` 注解实现方法级权限控制，支持 AND/OR 逻辑组合、数据权限、角色继承。权限定义存储在数据库，支持动态刷新。

**优点**：

- 注解驱动，权限规则与业务代码分离
- 支持 AND/OR 逻辑运算符，满足复杂权限组合
- 数据权限支持行级过滤
- 权限热加载，修改后无需重启

**缺点**：

- 权限模型与 Spring Security 深度耦合，升级 Spring Security 需同步适配
- 缺少权限变更审计日志
- 权限规则复杂度高时，注解嵌套层级深，可读性下降

---

## 与主流方案的生产力对比

### 异步任务场景

| 特性 | 传统 `@Async` + 手写 | Spring Cloud Stream | RocketMQ 事务消息 | **boot-architect** |
|------|:--:|:--:|:--:|:--:|
| 接入成本 | 高（建表+Job+重试） | 中 | 中 | **低（1 注解）** |
| 事务绑定 | 手写 | ❌ | ✅ | ✅ |
| 自动重试 | 手写 | ❌ | ✅ | ✅ |
| 死信机制 | 手写 | ❌ | ❌ | **✅ 内置** |
| 版本控制 | ❌ | ❌ | ❌ | **✅ 独有** |
| 外部中间件 | 无 | 需 MQ | 需 RocketMQ | **仅 MySQL** |

### 领域事件场景

| 特性 | Spring 事件机制 | Spring Cloud Stream | **boot-architect** |
|------|:--:|:--:|:--:|
| 持久化 | ❌ 内存 | ❌ | **✅ 本地表** |
| 跨服务 | ❌ | ✅ | ✅ |
| 多 MQ 切换 | — | 改绑定器 | **改配置** |
| 幂等消费 | 手写 | 手写 | **✅ 内置** |
| 补偿机制 | ❌ | ❌ | **✅ 补偿表** |
| 领域对象侵入 | 需注入 Publisher | 需注入 | **无需 Spring Bean** |

### 缓存场景

| 特性 | `@Cacheable` | 手写 RedisTemplate | **boot-architect** |
|------|:--:|:--:|:--:|
| 本地缓存 | ❌ | 手写 | **✅ Caffeine 内置** |
| 分布式缓存 | 需配置 | 手写 | **✅ Redisson 内置** |
| 缓存双删 | ❌ | 手写 | **✅ 自动** |
| 热 Key 保护 | ❌ | ❌ | **✅ 自动检测** |
| 缓存穿透防护 | ❌ | 手写 | **✅ 内置** |

### Web 层开发

| 特性 | 传统 Spring MVC | **boot-architect** |
|------|:--:|:--:|
| 统一响应体 | 每方法手写 `ApiReturn.success()` | **`@ApiBody` 自动包装** |
| 异常处理 | 手写 `@ExceptionHandler` | **14 种异常自动拦截** |
| 响应加密 | 手写 Filter | **`@ApiBody(encrypt=true)`** |
| API 版本路由 | 手写 URL 前缀 | **`@ApiVersion` 声明式** |
| 枚举参数转换 | 手写 Converter | **自动映射** |

---

## 架构优势总结

### 1. 方法论统一

6 个模块共享同一套设计范式：**注解驱动 + 自动装配 + 拦截/增强 + 业务无感知**。新人掌握一个模块后，其他模块学习成本极低。

### 2. 渐进式可拔插

每个模块独立依赖、独立自动配置。不需要异步事务则不加 `architect-transaction`，不需要领域事件则不加 `architect-event`，按需引入。

### 3. 低外部依赖

核心能力仅依赖 MySQL + Spring Boot，不强制绑定特定 MQ 或中间件。MQ 在需要跨服务投递时才引入。

### 4. 生产验证充分

10 条业务线全量使用，可靠性、兼容性、升级平滑性经过了规模化检验，不是 Demo 级别。

---

## 改进建议（按优先级排序）

### 高优先级

| 建议 | 说明 |
|------|------|
| 增加监控 Dashboard | 任务堆积量、成功率、重试趋势可视化，降低运维成本 |
| 死信任务管理 API | 提供 REST API 查询/重投/删除死信，避免手动操作数据库 |
| 缓存统计指标 | 暴露命中率、加载耗时等 Metrics，便于性能调优 |

### 中优先级

| 建议 | 说明 |
|------|------|
| 事件存储扩展 | 增加 Redis/MongoDB 存储实现，降低 MySQL 写压力 |
| 缓存预热机制 | 应用启动时自动加载热点数据到 Caffeine |
| 请求链路追踪 | Web 模块集成 TraceId，串联异步任务和事件链路 |
| 参数序列化 SPI | 事件/事务模块支持替换 fastjson2 为 Jackson/Kryo |

### 低优先级

| 建议 | 说明 |
|------|------|
| 事件版本化/重放 | 支持事件溯源和任意时间点重放 |
| 嵌套对象变更追踪 | Aggregate 支持任意深度的嵌套对象 diff |
| 批量任务聚合 | 高并发下多个同类型异步任务合并执行 |
| 泛型集合参数支持 | 使用 `TypeReference` 替代 `Class<?>` 解决泛型擦除 |

---

## 文档信息

- **生成日期**：2026-05-09
- **审查范围**：architect-transaction、architect-event、architect-cache、architect-aggregate、architect-webmvc-boot-starter、architect-webmvc-security
- **审查依据**：阿里巴巴 Java 开发手册、DDD 战术模式最佳实践、Spring Boot 生态最佳实践
