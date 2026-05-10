# boot-architect 中间件体系说明文档

## 项目概述

boot-architect 是一套基于 Spring Boot 深度封装的基础设施中间件体系，覆盖 **22 个模块**，经过 **10 条业务线全量生产验证**。全部模块共享统一设计范式：

```
注解标记（声明式）→ 自动装配（约定优于配置）→ 拦截/增强（AOP）→ 业务无感知
```

### 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.5 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Java | 25 |
| Maven | 多模块聚合 |

### 模块全景图

```
architect-bom (版本管理中心)
│
├─ architect-commons ──────────── 公共工具库（加密/HTTP/校验/分页/触发器）
├─ architect-spring ──────────── Spring 扩展（策略执行器框架）
│
├─ 数据层
│  ├─ architect-redisson ────── Redis 自动配置
│  ├─ architect-cache ──────── Caffeine + Redisson 二级缓存
│  ├─ architect-mybatis ────── MyBatis / Plus / Flex 扩展
│  └─ architect-aggregate ──── DDD 聚合模式（快照比对 + 增量 UPDATE）
│
├─ Web 层
│  ├─ architect-webmvc-boot-starter ─ 异常处理/响应包装/加密/版本路由
│  ├─ architect-webmvc-security ────── 方法级权限控制（vs Spring Security）
│  ├─ architect-webmvc-jackson ────── Jackson 3 序列化配置
│  ├─ architect-webmvc-fastjson2 ──── fastjson2 序列化配置
│  ├─ architect-webmvc-swagger ────── springdoc-openapi 枚举增强
│  └─ architect-webmvc-webtoken ───── Web Token 认证
│
├─ 消息与事件层
│  ├─ architect-event ────────── 领域事件 + 5 种 MQ 投递
│  ├─ architect-rocketmq ─────── RocketMQ（v5 + ONS）集成
│  └─ architect-pulsar ───────── Pulsar 消息队列（骨架）
│
├─ 安全与日志层
│  ├─ architect-token ────────── 认证令牌/会话/禁言/二次验证框架
│  ├─ architect-idempotent ──── 接口幂等（MySQL / Redis）
│  ├─ architect-duplicate ────── 数据去重校验
│  ├─ architect-mutex-lock ───── 分布式互斥锁（MySQL / Redis）
│  ├─ architect-bizlog ───────── 业务操作日志（MySQL/MongoDB/ES）
│  └─ architect-operate ──────── 系统操作审计日志
│
├─ 事务与调度层
│  ├─ architect-transaction ──── 异步事务日志（@TxAsync）
│  └─ architect-scheduler ───── 分布式任务调度（骨架）
│
└─ 集成层
   ├─ architect-aliyun ───────── 阿里云 OSS + 短信 + 号码认证
   ├─ architect-ip2region ────── IP 离线定位
   └─ architect-search ───────── 搜索引擎（骨架）
```

---

## 1. architect-bom — 版本管理中心

**路径**: `architect-bom/`  
**类型**: POM (packaging=pom)，无 Java 代码

统一管理所有模块及 40+ 第三方依赖的版本号。子模块通过 `dependencyManagement` 继承，无需显式声明版本。

```xml
<!-- 核心版本 -->
<spring-boot.version>3.5.5</spring-boot.version>
<spring-cloud.version>2025.0.0</spring-cloud.version>
<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
<dubbo.version>3.2.19</dubbo.version>

<!-- 持久化 -->
<mybatis-spring-boot.version>3.0.5</mybatis-spring-boot.version>
<mybatis-plus.version>3.5.12</mybatis-plus.version>
<mybatis-flex.version>1.11.5</mybatis-flex.version>
<druid.version>1.2.27</druid.version>

<!-- 缓存与消息 -->
<redisson.version>4.2.0</redisson.version>
<ons-client.version>1.9.1.Final</ons-client.version>
<elasticsearch.version>8.3.2</elasticsearch.version>

<!-- 工具库 -->
<fastjson2.version>2.0.61</fastjson2.version>
<hutool.version>5.8.40</hutool.version>
<knife4j.version>4.5.0</knife4j.version>
```

BOM 通过 `scope=import` 导入 Spring Boot / Spring Cloud / Spring Cloud Alibaba 的官方 BOM，形成多层级版本继承体系。

---

## 2. architect-commons — 公共工具库

**路径**: `architect-commons/`  
**类型**: 单模块，24 个源文件  
**依赖**: fastjson2、guava、commons-lang3、bcprov、httpclient5、hibernate-validator（无 Spring Boot Starter）

### 包结构

#### 2.1 枚举体系 (`com.cloud.arch.enums`)

| 类 | 功能 |
|----|------|
| `Value<T>` | 统一枚举接口，`value()` / `label()` / `equal()` / `ofNullable()` / `valueOf()` |
| `ValueType` | 枚举值类型处理器，支持 7 种基础类型（BYTE~STRING），提供 compareTo/fromValue |
| `EnumValue<K,V>` | 枚举包装类，接收枚举 Class 自动构建 `Map<K,V>` 查询映射 |
| `Transition<K,T>` | 状态机过渡接口，继承 `Value`，增加 `transitions()` 方法支持状态流转 |

#### 2.2 分页查询 (`com.cloud.arch.page`)

| 类 | 功能 |
|----|------|
| `Pager<T>` | 通用分页结果，含 `total`/`current`/`pageSize`/`records`，提供 `map()`/`flatMap()`/`forEach()` |
| `PageCondition` | 分页条件构建器，`count()` + `query()` 方法链 |
| `PageQuery` | 自动反射解析查询条件，排除 `@Ignore` 字段，支持 `@Alias` 别名 |
| `@Alias` | 字段别名注解 |
| `@Ignore` | 忽略字段注解 |

#### 2.3 HTTP 客户端 (`com.cloud.arch.http`)

| 类 | 功能 |
|----|------|
| `HttpRequest` | 基于 Apache HttpClient5，单例模式，支持 GET/POST/PUT，连接池（20 连接），form/json/xml/bytes/download |
| `RequestWrapper` | Builder 模式链式设置 header/entity/params |
| `HttpRequestException` | 5 种异常分类（连接/响应/中断/握手/主机） |

#### 2.4 加密工具 (`com.cloud.arch.encrypt`)

| 类 | 功能 |
|----|------|
| `AESKit` | AES 加解密，ECB/CBC 模式，PKCS5/PKCS7/NoPadding 填充，`genKey()` 24 位 |
| `RSAKit` | RSA 加解密（1024 位），公钥/私钥加解密、数字签名，分段处理大消息 |

#### 2.5 通用工具 (`com.cloud.arch.utils`)

| 类 | 功能 |
|----|------|
| `IdWorker` | 雪花算法分布式 ID，workerId = IP + 进程 PID 计算，`nextId()`、`uuid()`（62 进制） |
| `JsonUtils` | Fastjson2 包装，toBean/toJson/readValue/readList/readMap |
| `CollectionUtils` | 200+ 行集合工具，isEmpty/toList/toSet/toMap/groupBy/counting/flatMap/findFirst |
| `SleepyTask` | 可唤醒异步任务抽象，`AtomicBoolean` CAS 实现 |
| `SingleFlight<K,R>` | 协程式防重复调用并发控制，同一 key 的并发请求只执行一次 |
| `SpElExpressionParser` | 通用 SpEL 表达式解析器 |

#### 2.6 缓冲触发器 (`com.cloud.arch.trigger`)

| 类 | 功能 |
|----|------|
| `BufferedTrigger<E>` | 缓冲队列触发消费器，`publish()`/`batchSize`/`timeout`，支持单消费者和多消费者策略 |
| `ConsumerListener<E>` | 消费监听器，`handle(List<E> events)` 批量消费 |

---

## 3. architect-spring — 策略执行器框架

**路径**: `architect-spring/`  
**类型**: 单模块，6 个源文件  
**依赖**: spring-context、guava、architect-commons

### 核心机制

基于 `@ExecPoint` 注解 + `ClassIndex` 编译期索引的策略模式框架，实现业务逻辑的自动路由。

### 核心类

| 类/接口 | 功能 |
|----------|------|
| `Executor<K>` | 泛型接口，`bizIndex()` 返回业务标识 K |
| `@ExecPoint` | 标记执行器实现类，编译期通过 `ClassIndex` 收集索引 |
| `ExecutorFactory` | 全局执行器工厂，`SmartInitializingSingleton` 扫描所有 `@ExecPoint` Bean，按泛型类型分组存储 |
| `EnumExecutorFactory<K,E>` | 基于 EnumMap 的执行器工厂，K 强制为枚举 |
| `CommonExecutorFactory<K,E>` | 基于 HashMap 的通用执行器工厂 |

### 使用示例

```java
@ExecPoint
@Component
public class SmsSender implements Executor<String> {
    @Override
    public String bizIndex() { return "sms"; }
}

// 调用方
executorFactory.of("sms").execute(...);
```

---

## 4. architect-redisson — Redis 自动配置

**路径**: `architect-redisson/`  
**类型**: 单模块

封装 Redisson 客户端的 Spring Boot 自动装配，支持单机、哨兵、集群、主从四种模式。

### 核心类

| 类 | 功能 |
|----|------|
| `RedissonAutoConfiguration` | 自动配置入口，创建 `RedissonClient` Bean |
| `RedisConfig` | 配置属性绑定 |

### 配置示例

```yaml
com.cloud.redis.single:
  address: "redis://127.0.0.1:6379"
  password: ""
  database: 0
  connection-pool-size: 64
```

---

## 5. architect-cache — 二级缓存系统

**路径**: `architect-cache/`  
**类型**: 3 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-cache-support` | 核心抽象层（Cache 接口、Caffeine/Redis 实现、指标体系） |
| `architect-cache-boot-starter` | 自动配置层（AOP 拦截、SpEL 解析、延迟双删） |
| `architect-hotkey` | 热 Key 检测扩展（基于 JD HotKey + ETCD） |

### 核心注解

| 注解 | 功能 | 关键参数 |
|------|------|---------|
| `@CacheResult` | 读缓存（穿透后加载回填） | `names`、`key`(SpEL)、`condition`、`unless` |
| `@CachePut` | 写缓存（同步更新 L1+L2） | 同上 |
| `@CacheEvict` | 清除缓存（延迟双删+MQ 广播） | `allEntries`、`beforeInvocation` |
| `@CacheAction` | 类级别缓存默认配置 | `names`、`keyGenerator`、`cacheResolver` |
| `@Local` | 本地缓存参数 | `initialSize`、`maximumSize`、`expire`、`expireMode` |
| `@Remote` | 远程缓存参数 | `expire`、`randomBound`、`magnification`、`preloadTime` |

### 三级防护

| 问题 | 解决方案 |
|------|---------|
| **缓存穿透** | 空值包装（DB 返回 null 时包装为 `NullValue` 存入缓存，后续查询直接返回缓存的空值）+ 短 TTL（`magnification` 除数，默认 /3） |
| **缓存击穿** | JVM 级 `MapMaker.weakValues()` per-key 锁 + 集群级 Redisson `RLock` 两阶段重试（先 get 5 次再抢锁） |
| **缓存雪崩** | `ThreadLocalRandom.nextInt(randomBound)` 随机偏移 TTL（默认 0~1200s） |

### 延迟双删机制

1. `@TransactionalEventListener` 在事务提交后删除 L2 + L1
2. `DelayQueue` 延迟 500ms 后执行第二次删除（覆盖数据库主从延迟窗口）
3. Redis Pub/Sub 广播 `RefreshEvent` 通知其他节点同步清除 L1
4. 节点通过 `CacheNodePolicy` 跳过自身发布的消息，防止无限循环

### 热 Key 检测

基于 JD HotKey 滚动桶算法自动统计 Key 访问频率，达到阈值后通过 ETCD 广播全集群提升到 L1 本地缓存。

### 配置

```yaml
com.cloud.cache:
  enable-metric: true
  enable-delay-evict: true
  delay-evict-interval: 500ms
```

---

## 6. architect-mybatis — MyBatis 扩展体系

**路径**: `architect-mybatis/`  
**类型**: 4 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-mybatis-commons` | 公共核心：类型处理器（JSON/枚举/IP）+ `@TypeHandler` 注解 |
| `architect-mybatis-extension` | 原生 MyBatis 自动配置 |
| `architect-mybatis-plus-extension` | MyBatis-Plus 扩展（主键生成/乐观锁/分页/时间填充） |
| `architect-mybatis-flex-extension` | MyBatis-Flex 扩展 |

### architect-mybatis-commons

核心注解 `@TypeHandler` + `@Inherited`，通过 `ClassIndex` 编译期索引自动发现标注了 `@TypeHandler` 的枚举/JSON 类。

| 类 | 功能 |
|----|------|
| `TypeHandlerRegister` | 通过 `ClassIndex` 扫描 `@TypeHandler`，注册所有类型处理器 |
| `JsonTypeHandler<T>` | JSON ↔ String 转换（fastjson2） |
| `EnumTypeHandler` | `Value` 接口枚举 ↔ 数据库值转换 |
| `IpTypeHandler` | IPv4 字符串 ↔ int 转换 |

### architect-mybatis-plus-extension

| 类 | 功能 |
|----|------|
| `MybatisPlusConfiguration` | 注册类型处理器/主键生成器/乐观锁拦截器/分页拦截器/时间填充器 |
| `CustomIdGenerator` | 雪花 ID 主键生成 |
| `TimeMetaObjectHandler` | insert 自动填充创建时间+更新时间，update 填充更新时间 |
| `Query<T>` | 扩展 `AbstractWrapper`，集成 BaseMapper 直接执行 CRUD + 分页，`pager()` 返回 `Pager<T>` |
| `LambdaQuery<T>` | Lambda 风格 Query，`SFunction` 类型安全列名 |

### architect-mybatis-flex-extension

| 类 | 功能 |
|----|------|
| `MybatisFlexConfiguration` | 注册类型处理器 + 全局主键生成器 |
| `WorkerIdGenerator` | 雪花 ID 键生成器 |
| `Query<T>` | 扩展 `QueryWrapperAdapter`，`where(Condition)`/`list()`/`pager()`/`update()`/`delete()` |
| `Condition` | 函数式接口 `Consumer<QueryWrapper>` |

---

## 7. architect-aggregate — DDD 聚合模式

**路径**: `architect-aggregate/`  
**类型**: 单模块（15 个 Java 文件），编程式 API（无 Spring 注解）

### 核心能力

**快照比对 → 增量 UPDATE**。`Aggregate` 构造时通过 `ForyDeepCopier` 深拷贝聚合根生成 snapshot，`changed()` 反射遍历字段用 `DeepEquals` 逐一比对 root vs snapshot，返回仅含变更字段的聚合根副本。仓储层据此生成 `UPDATE SET a=?, b=? WHERE id=?` 而非全量 UPDATE。

### 核心类与接口

| 类/接口 | 类型 | 功能 |
|----------|------|------|
| `Aggregate<K, R>` | 泛型类 | 聚合模式核心：K=主键类型，R=聚合根类型（extends `AggregateRoot<K>`） |
| `AggregateRoot<K>` | 接口 | 聚合根标记接口，继承 `Entity<K>` |
| `Entity<I>` | 接口 | 实体接口，定义 `getId()`/`setId()`，`isNew()` 默认实现为 `version == 0` |
| `AggregateFactory` | 工厂类 | 聚合工厂，`create(root)` / `create(root, copier)` 创建 `Aggregate` 实例 |
| `DeepCopier` | 接口 | 深拷贝抽象：`<T extends Entity> T copy(T source)` |
| `ForyDeepCopier` | 实现类 | `DeepCopier` 实现，基于 `org.apache.fory.ThreadSafeFory`，`CopierHolder` 静态内部类单例 |
| `Repository<K, R>` | 接口 | 仓储抽象：`save(Aggregate<K,R>)` |
| `Lazy<T>` | 工具类 | 线程安全延迟加载（DCL + volatile），实现 `Supplier<T>` |
| `@Ignore` | 注解 | 字段级注解，`String[] group() default {}`，标记不参与变更比对的字段 |
| `CompareResult<V>` | 工具类 | 集合比对结果（新增/修改/删除），由静态方法 `Aggregate.compare()` 生成 |
| `DeepEquals` | 工具类 | 基于反射的深度相等比较（字段逐项递归，防循环引用） |
| `ReflectionUtils` | 工具类 | 反射工具，`getDeepDeclaredFields()` 递归获取类层次的所有字段（含父类） |

> **注意**：该模块不存在 `AbstractContainer` 类。子实体集合的变更追踪通过 `Aggregate` 内建的实体集合方法（`getNewEntities`/`changedEntities`/`removedEntities`）配合 `Aggregate.compare()` 实现。

### Aggregate 核心方法

```java
// === 变更检测 ===
R changed()                              // 返回仅含变更字段的聚合根副本（未变更返回 null）
R changed(String group)                  // 按分组变更（@Ignore(group) 过滤），未变更返回 null
Optional<R> ifChanged()                  // Optional 包装 changed()
Optional<R> ifChanged(String group)      // Optional 包装 changed(group)
boolean  hasChanged()                    // 通过 DeepEquals 判断是否有任何字段变更
boolean  isNew()                         // 委托给 Entity.isNew()（version == 0 为新实体）
Set<String> changedFields()              // 获取变更字段名集合
Set<String> changedFields(String group)  // 按分组获取变更字段名

// === 实体集合 CRUD 追踪 ===
<I,T extends Entity<I>> List<T> getNewEntities(Function<R, Collection<T>> getCollection)
<I,T extends Entity<I>> List<T> changedEntities(Function<R, Collection<T>> getCollection)
<I,T extends Entity<I>> List<T> removedEntities(Function<R, Collection<T>> getCollection)
<I,T extends Entity<I>> List<T> newEntities(Function<R, Collection<T>> getCollection)
<V> CompareResult<V> compare(Collection<V> newValues, Collection<V> oldValues)  // 静态方法

// === 子实体检索 ===
<I,T extends Entity<I>> Optional<T> changedEntity(Function<R, T> loader)

// === 链式 API ===
Aggregate<K,R> peek(Consumer<R> consumer)       // 对 root 执行 consumer，返回 this 链式
<T> T          map(Function<R, T> action)       // 对 root 执行 function，返回结果
Aggregate<K,R> save()                           // 通过 Repository 持久化（需构造时传入 Repository）
Aggregate<K,R> save(Consumer<Aggregate<K,R>> c) // 自定义持久化逻辑
```

### @Ignore 注解

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
    String[] group() default {};  // 空数组 = 所有分组都忽略；指定分组名 = 仅该分组忽略
}
```

忽略逻辑：`group` 为空时所有 `@Ignore` 字段都被跳过；指定 `group` 时仅 `@Ignore` 注解的 `group()` 数组包含该分组的字段被跳过。

### 内部实现细节

- **`scanChangedFields`**：私有方法，通过 `ReflectionUtils.getDeepDeclaredFields()` 获取所有层次字段，用 `shouldIgnore(field, group)` 过滤，`DeepEquals.deepEquals()` 逐字段比对，通过内部 `FieldCollector`（`@FunctionalInterface`）收集变更字段值
- **`ForyDeepCopier`**：`CopierHolder` 静态内部类单例模式（Holder 模式），构建参数 `Language.JAVA` + `refCopy(false)` + `requireClassRegistration(false)`，底层 `ThreadSafeFory` 自身线程安全
- **`Lazy<T>`**：Double-Checked Locking + volatile 保证可见性，`supplier` 和 `result` 均为 volatile，`forceEagerEvaluation()` 中 `synchronized(this)` 双重检查

---

## 8. architect-webmvc — Web 层深度定制

**路径**: `architect-webmvc/`  
**类型**: 7 个子模块

### 8.1 architect-webmvc-boot-starter（Web 增强核心）

**核心注解**

| 注解 | 位置 | 功能 |
|------|------|------|
| `@ApiBody` | 方法/类 | 响应体自动包装 + AES 加密（CBC/ECB），通过 `while (method != null)` 循环向上查找类和方法上的注解 |
| `@ApiVersion` | 方法/类 | API 版本路由，正则 `(\d\.)+\d` 从 URL 提取版本号，基于 Spring `RequestCondition` |

**异常处理双优先级链**

```
WebmvcHandlerAdvice (HIGHEST_PRECEDENCE)
  ├─ MethodArgumentNotValidException → 参数校验失败（JSR-303）
  ├─ HttpMediaTypeNotSupportedException → 不支持的 Content-Type
  ├─ HttpRequestMethodNotSupportedException → 不支持的 HTTP 方法
  ├─ BindException / MethodArgumentTypeMismatchException → 参数绑定/类型错误
  ├─ HttpMessageNotReadableException → 请求体不可读
  ├─ MissingServletRequestParameterException → 缺少必填参数
  ├─ IllegalArgumentException → 业务参数校验
  └─ MaxUploadSizeExceededException → 上传文件超限

GenericHandlerAdvice (LOWEST_PRECEDENCE)
  └─ 兜底处理未分类异常
```

**关键设计**

- `WebMvcRegistrations`：替换 `RequestMappingHandlerAdapter` 植入 Advice，避免传统 `@ControllerAdvice` 注册时序问题
- 响应加密：AES CBC/ECB，密钥通过 RSA 公钥加密后放入 Response Header
- 字典端点：`DictionaryEndpoint` 实现 `InitializingBean`，反射扫描 `@ApiDict` 动态注册路由
- 枚举参数自动转换：`EnumConverterFactory`（按 name/ordinal/index 三种模式）
- 日期参数自动识别：4 种格式（`yyyy-MM-dd`、`yyyy-MM-dd HH:mm:ss`、`yyyy-MM-dd HH:mm`、时间戳）

### 8.2 architect-webmvc-security（方法级权限）

零外部依赖的自研轻量级权限框架。详见 [MODULE_EVALUATION.md](./MODULE_EVALUATION.md)。

**核心注解**

```java
@Permission(
    domain = "*",              // 访问域
    permit = {"user:create"},  // 权限标识
    role = {"admin"},          // 角色标识
    mode = GrantMode.AND       // AND/OR 逻辑
)
```

**双通道拦截**

```
AOP（注解优先）→ Interceptor（URI 路由备选）
```

- 方法/类标注 `@Permission` → `StaticMethodMatcherPointcutAdvisor`（`HIGHEST_PRECEDENCE`）拦截
- 未标注 → `UriResourceAuthorizeInterceptor` 按配置的 URI 模式匹配
- `GrantAuthority.decide()` 用 `Sets.intersection` 做角色/权限交集判断
- `AuthorizeCacheManager` Caffeine 缓存授权结果（三元组 key = domain + identity + elementKey）

**配置**

```yaml
com.cloud.web.security:
  enable: true
  cached: true
  resources:
    - /user/** | post,put | * | permit(user:create) and role(system)
    - /job/execute | get | system | permit(job:write)
```

### 8.3 其他 Web 子模块

| 子模块 | 功能 |
|--------|------|
| `architect-webmvc-commons` | 通用错误处理、HTTP 状态码映射 |
| `architect-webmvc-jackson` | Jackson 3 序列化配置（枚举值序列化、日期格式） |
| `architect-webmvc-fastjson2` | fastjson2 序列化配置（替代 Jackson） |
| `architect-webmvc-swagger` | springdoc-openapi 枚举增强（@EnumValue 支持） |
| `architect-webmvc-webtoken` | Web Token 认证（Servlet + Gateway 两种模式） |

---

## 9. architect-event — 领域事件系统

**路径**: `architect-event/`  
**类型**: 10 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-event-commons` | 核心抽象（`@Publish`/`@Subscribe` 注解） |
| `architect-event-boot-starter` | 自动配置（事务同步、幂等消费、补偿调度） |
| `architect-event-core` | 发布/订阅核心引擎 |
| `architect-event-storage-jdbc` | JDBC 事件存储 |
| `architect-event-storage-rocksdb` | RocksDB 事件存储 |
| `architect-event-queue-rocketmq-v5x` | RocketMQ V5 适配 |
| `architect-event-queue-kafka` | Kafka 适配 |
| `architect-event-queue-rabbitmq` | RabbitMQ 适配 |
| `architect-event-queue-pulsar` | Pulsar 适配 |

### 核心注解

| 注解 | 位置 | 功能 |
|------|------|------|
| `@Publish` | 事件类 | 声明事件可发布（配置 bizGroup、name、filter、延迟时间） |
| `@Subscribe` | 消费者类 | 声明订阅关系（配置 group、name、filter、分片键），支持 `@Repeatable` |

### 架构流程

```
业务方法内                     事务边界                           MQ 投递
    │                             │                                │
    ▼                             ▼                                ▼
DomainEventPublisher.publish() → ThreadLocal 收集事件              ──┐
    │                             │                                  │
    ▼                             ▼                                  │
TransactionSynchronization    beforeCommit                         ──┤
                                  │ 批量 INSERT 落库（同事务）        │ 事务边界内
                                  ▼                                  │
                              afterCommit                            │
                                  │ 投递 MQ                         ──┘
                                  ▼
                          EventConcurrentlyListener (RocketMQ)
                          (或其他 MQ Listener)
                                  │
                                  ▼
                          EventSubscribeHandler.handle()
                          ├─ INSERT IGNORE 幂等检查（event_name + event_filter 唯一索引）
                          ├─ ApplicationEventPublisher.publishEvent() → 业务 @EventListener
                          └─ finally: markProcessed
```

### 关键设计

- **领域对象零侵入**：`DomainEventPublisher` 是 `@UtilityClass` 静态工具类，不需要注入任何依赖
- **按需装配**：发布端和订阅端通过 `@ConditionalOnProperty` 独立开关（只发不收、只收不发）
- **5 种 MQ 统一抽象**：`MessageQueuePublisher` 接口，切换只需改配置
- **幂等方案**：INSERT IGNORE + InnoDB 唯一索引，不依赖 Redis
- **失败补偿**：独立 `arch_event_compen` 补偿表 + 定时任务处理
- **乐观并发控制**：状态变更 SQL 均含 `version=:version` WHERE 条件

---

## 10. architect-rocketmq — RocketMQ 消息队列

**路径**: `architect-rocketmq/`  
**类型**: 3 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-rocketmq-commons` | 公共核心：10 个注解 + 元数据模型 + 幂等 + 事务消息 |
| `architect-rocketmq-ons-starter` | 阿里云 ONS 商业版 Starter |
| `architect-rocketmq-v5x-starter` | RocketMQ v5.x 开源版 Starter |

### 核心注解（architect-rocketmq-commons）

| 注解 | 目标 | 功能 |
|------|------|------|
| `@Producer` | TYPE | 标记生产者接口 |
| `@Sender` | METHOD | 标记发送方法（topic/tag/timeout/delay/batch/orderly/async/oneWay） |
| `@Consumer` | TYPE | 标记消费者类（group/model），复合 `@Component` |
| `@Listener` | METHOD | 标记消费监听方法（topic/tag/idempotent） |
| `@TxSender` | METHOD | 标记事务消息发送方法 |
| `@TxChecker` | TYPE | 标记事务状态回查器，复合 `@Component` |
| `@Payload` | PARAMETER | 标记消息体参数 |
| `@Key` | PARAMETER | 标记消息业务 Key 参数 |
| `@Delay` | PARAMETER | 标记延迟消息参数 |
| `@ShardingKey` | PARAMETER | 标记顺序消息分区 Key |

### 关键机制

- `@Producer` 标注的接口通过 `ImportBeanDefinitionRegistrar` + 类路径扫描自动注册为代理 Bean
- `@Sender` 方法被拦截后，根据注解属性通过 **策略模式**（`SenderRecogniseHandler`）路由到同步/异步/顺序/单向/批量/延迟等不同发送模式
- 事务消息通过 AOP（`TxSenderAnnotationPointcutAdvisor` + `TransactionSenderInterceptor`）拦截 `@TxSender` 方法，先发送半消息，执行业务逻辑后提交/回滚
- 幂等消费通过 `AbstractIdempotentCheck` 异常分类策略：retryFor（重试）、idempotentFor（标记成功）、其他异常（标记失败）

### 配置

```yaml
# RocketMQ v5.x
com.cloud.rocket.v5x:
  nameSrv: "127.0.0.1:9876"
  producer.enable: true
  consumer.enable: true

# 阿里云 ONS
com.cloud.rocket.ons:
  accessKey: "..."
  secretKey: "..."
  onsAddress: "..."
```

---

## 11. architect-pulsar — Pulsar 消息队列（骨架）

**路径**: `architect-pulsar/`  
**状态**: 🔴 骨架模块（仅有 `architect-pulsar-commons` 和 `architect-pulsar-boot-starter` 的 `package-info.java`，无实质代码）

---

## 12. architect-transaction — 异步事务日志

**路径**: `architect-transaction/`  
**类型**: 2 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-transaction-async-support` | 核心抽象层（`@TxAsync` 注解、仓储接口、编解码接口） |
| `architect-transaction-async-starter` | 自动配置层（AOP 拦截、事务同步、补偿/修复调度器） |

### 核心注解

```java
@TxAsync(name = "sendSms", version = "1.0", retryInterval = 30L, maxRetry = 8)
public void sendSms(String phone, String content) {
    smsGateway.send(phone, content);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | `""` | 任务名称，拼接到 asyncKey |
| `version` | String | `"1.0"` | 任务版本，代码升级时自动跳过旧版本事件 |
| `retryInterval` | long | `30L` | 重试间隔基数（秒），指数增长 |
| `maxRetry` | int | `8` | 最大重试次数，超过后标记 DEAD |

### 四层防护体系

```
afterCommit（business-executor）→ 即时执行
    │ 失败
    ▼
DelayQueue（retry-executor）→ 指数退避重试 30s→60s→120s→…→8次
    │ 仍失败
    ▼
AsyncCompensateScheduler → 分布式锁 + 定时扫描 FAIL 任务
    │ 僵死
    ▼
AsyncReparationScheduler → 修复 READY/RUNNING 异常任务
```

### 关键设计

- 线程池隔离：business-executor（首次执行 + 僵死修复）与 retry-executor（补偿重试）独立
- 版本控制：`AsyncTxVersion` 按数字段逐位比较（`x.x` 或 `x.x.x`），代码升级后自动跳过旧版本事件
- 分库分表：`shardKey` 贯穿所有 SQL，`afterCompletion` 自动清理上下文
- 幂等保证：UPDATE SQL 均带 `id + shard_key` 条件

### 配置

```yaml
com.cloud.async.transaction:
  batch: 50
  period: 60s
  business:
    core: 2
    max-size: 32
  retry:
    core: 1
    max-size: 4
```

---

## 13. architect-scheduler — 分布式任务调度（骨架）

**路径**: `architect-scheduler/`  
**状态**: 🔴 骨架模块（6 个子模块均仅有 `Main.java` 占位文件）

---

## 14. architect-bizlog — 业务操作日志

**路径**: `architect-bizlog/`  
**类型**: 5 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-bizlog-commons` | 核心抽象（`@OperateLog` 注解、`LogRecord` 实体、函数工厂） |
| `architect-bizlog-boot-starter` | 自动配置 + AOP 拦截 |
| `architect-bizlog-mysql` | MySQL 存储实现 |
| `architect-bizlog-mongodb` | MongoDB 存储实现 |
| `architect-bizlog-elasticsearch` | Elasticsearch 存储实现 |

### 核心注解

```java
@OperateLog(
    group = "order",
    bizNo = "#order.id",
    success = "创建订单成功，金额：{#order.amount}",
    failure = "创建订单失败：{#_error}",
    detail = "操作人：{#_operator}",
    condition = "#order.amount > 0"
)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `group` | String | 业务分组 |
| `bizNo` | String | 业务编号（SpEL） |
| `success` | String | 成功日志模板（SpEL），支持 `{FUNC_NAME{spel}}` 自定义函数 |
| `failure` | String | 失败日志模板 |
| `tenant` | String | 租户标识（SpEL） |
| `operator` | String | 操作者（SpEL） |
| `detail` | String | 操作详情（SpEL） |
| `condition` | String | 过滤条件（SpEL，结果为 true 才记录） |

### 核心机制

1. AOP 拦截 `@OperateLog` 方法
2. 方法执行前：遍历模板表达式，执行 `beforeInvoke=true` 的自定义函数（如获取操作者）并缓存
3. 执行目标方法，捕获异常和返回值
4. 方法执行后：执行全部 SpEL 表达式，`_return` 和 `_error` 变量注入上下文，渲染模板
5. `ProxyLogRepository` 代理：异步模式下委托 `AsyncLogDispatcher`（基于 `BufferedTrigger` 批量消费），否则同步保存

### 扩展点

实现 `INamedFunction` 接口并注册到 `LogFunctionContainer`，支持 `{FUNC_NAME{spel}}` 模板中引用自定义函数。

### 配置

```yaml
com.cloud.logger:
  async: true
  core-threads: 2
  max-threads: 8
  batch-size: 50
  timeout: 5s
```

---

## 15. architect-operate — 系统操作审计日志

**路径**: `architect-operate/`  
**类型**: 3 个子模块

区别于 architect-bizlog（面向任意方法的通用日志），architect-operate 专门面向 **Web 请求级** 的系统操作审计。

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-operate-commons` | 核心抽象（`@OpLog` 注解、`OperationLog` 实体、21 种 `OperateType`） |
| `architect-operate-spring-starter` | 自动配置 + AspectJ AOP + 异步批量写入 |
| `architect-operate-endpoint` | REST API 查询端点（`GET /oper/log`、`GET /oper/log/list`） |

### 核心注解

```java
@OpLog(
    bizGroup = "order",
    type = OperateType.ADD,
    title = "创建订单",
    excludes = {"password", "token"}
)
```

`OperateType` 枚举（21 种）：ADD、EDIT、DELETE、RECOVER、CANCEL、REVOKE、CLEAR、IMPORT、EXPORT、UPLOAD、DOWNLOAD、ISSUE、ACTIVE、TAKE_UP、TAKE_OFF、LOOK、OTHER

### 核心机制

1. AspectJ AOP 拦截 `@OpLog` 方法
2. 从 `RequestContextHolder` 获取 HTTP 请求上下文（URI、IP、operatorId 从 `AUTH_IDENTITY_HEADER` 请求头）
3. 执行方法计时（Stopwatch），记录异常
4. `AsyncLogDispatcher`（`BufferedTrigger`）异步批量消费：批量解析操作人名称、IP 地理位置（architect-ip2region）
5. 写入 `sys_oper_log` 表（IP 通过 `inet_aton` 整数存储，查询时 `inet_ntoa` 还原，支持脱敏）
6. 提供 REST API 查询操作日志，支持 IP 掩码脱敏、租户强制校验

### 扩展点

实现 `IOperatorResolver`（操作人名称解析）和 `ITenantResolver`（租户解析）接口并注册 Bean，框架自动发现。

---

## 16. architect-duplicate — 数据重复校验

**路径**: `architect-duplicate/`  
**类型**: 单模块

### 核心注解

```java
// 标记字段
@RptField(table = "t_user", column = "mobile", message = "手机号已存在")
private String mobile;

// 标记方法
@RptCheck
public void createUser(User user) { ... }
```

| 注解 | 参数 | 功能 |
|------|------|------|
| `@RptCheck` | 无 | 标记方法，遍历所有入参执行重复校验 |
| `@RptField` | `table`/`column`/`constraints`/`message` | 标记实体字段，声明校验的表名和列名 |

### 核心机制

1. AOP 拦截 `@RptCheck` 方法，遍历所有入参
2. 反射扫描每个参数的 `@RptField` 标记字段
3. 构建 SQL：`select 1 from {table} where {column}=:value [and constraint1=:val1 ...] limit 1`
4. 查询结果 > 0 则抛 `ApiBizException(400, message)`
5. 元数据 Class 级别缓存，避免重复反射
6. 同时提供编程式 API：`RptCheckService.check(target, constraints)`

---

## 17. architect-idempotent — 接口幂等

**路径**: `architect-idempotent/`  
**类型**: 4 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-idempotent-support` | 核心抽象（`@Idempotent` 注解、`IdempotentManager` 接口） |
| `architect-idempotent-starter` | 自动配置 + AspectJ AOP |
| `architect-idempotent-mysql` | MySQL 实现（`INSERT IGNORE` + 事务） |
| `architect-idempotent-redis` | Redis 实现（`SET NX PX`） |

### 核心注解

```java
@Idempotent(
    prefix = "order",
    key = "#order.id",
    expireTime = 10,
    timeUnit = TimeUnit.SECONDS,
    message = "请勿重复提交"
)
public void createOrder(Order order) { ... }
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `prefix` | String | `""` | Key 前缀，最终格式 `idm:{prefix}:{spelValue}` |
| `key` | String | 必填 | 幂等 Key（SpEL） |
| `sharding` | String | `""` | 数据库分片键（SpEL） |
| `expireTime` | long | `10` | Redis 锁过期时间 |
| `timeUnit` | TimeUnit | `SECONDS` | 时间单位 |
| `removeNow` | boolean | `false` | 方法完成后是否立即删除 Redis Key |
| `message` | String | `"repeat operate..."` | 重复提示 |

### 核心机制

1. AspectJ AOP 拦截 `@Idempotent` 方法
2. 通过 SpEL 解析 key（如 `#order.id`）和 sharding
3. **MySQL 模式**：开启编程事务 + `INSERT IGNORE INTO arch_idempotent`，成功则获取锁，方法完成后 commit（释放）或 rollback
4. **Redis 模式**：`SET NX PX` 原子操作，TTL 过期自动释放，支持 `removeNow` 立即删除
5. 获取锁失败返回 HTTP 429

---

## 18. architect-mutex-lock — 分布式互斥锁

**路径**: `architect-mutex-lock/`  
**类型**: 4 个子模块

### 子模块

| 子模块 | 定位 |
|--------|------|
| `architect-mutex-lock-support` | 核心抽象层（竞争模型、锁模型、定时调度器） |
| `architect-mutex-lock-starter` | 自动配置 + `MutexTemplate` 统一入口 |
| `architect-mutex-lock-mysql` | MySQL 实现（版本号乐观锁 + 事务） |
| `architect-mutex-lock-redis` | Redis 实现（Lua 脚本 + PubSub 通知 + ZSET 等待队列） |

### 通用竞争模型

```
contenderId = seq:pid@ip (HOST 模式，全局唯一)

时间线:
  |──── ttl (持有时间 10s) ────|──── transition (过渡缓冲 6s) ────|
  ↑                              ↑                                  ↑
  acquired                  续期窗口                          竞争者可抢占
```

- 持有者在 ttl 内可续期，`transition` 内其他竞争者不能抢占
- `ContendPeriod` 延迟策略：持有者延迟 = TTL 到期前，竞争者延迟 = Transition 到期后 + 随机偏移（避让雷群效应）

### MySQL 模式

原子竞争 SQL：
```sql
UPDATE arch_mutex SET owner_id=?, ttl_at=now+ttl, transition_at=now+ttl+transition
WHERE (transition_at < now) OR (owner_id=? AND transition_at > now)
```

`ScheduledThread` 单线程轮询竞争，版本号乐观锁保证原子性。

### Redis 模式

Lua 原子脚本 + Redis PubSub 通知机制：

| Lua 脚本 | 功能 |
|----------|------|
| `mutex_acquire.lua` | `SET NX PX` 竞争，成功则 PUBLISH `acquired` 事件；失败则将 contender 加入 ZSET 等待队列 |
| `mutex_guard.lua` | 续期守卫：持有者为自己则重新 SET NX PX 延长 TTL |
| `mutex_release.lua` | 释放：DEL 锁，从 ZSET 取下一个等待者，PUBLISH `released` 通知到专属 channel |

### 使用方式

```java
// Lock 模式
Lock lock = mutexTemplate.mutexLock("order-pay-123");
lock.acquire();
try { /* 业务 */ } finally { lock.close(); }

// 定时调度模式
mutexTemplate.scheduleAtRate("daily-job", 10, 3600, TimeUnit.SECONDS, task);
```

---

## 19. architect-token — 认证令牌框架

**路径**: `architect-token/`  
**类型**: 8 个子模块

### 子模块

| 子模块 | 状态 | 定位 |
|--------|------|------|
| `architect-token-support` | ✅ 已实现 | 核心模型 + 接口 + 注解 + 配置（27 个文件） |
| `architect-token-redis` | ✅ 已实现 | Redis（Redisson）存储实现 |
| `architect-token-boot-starter` | 🔴 骨架 | Starter 入口 |
| `architect-token-jwt` | 🔴 骨架 | JWT Token 实现 |
| `architect-token-gateway` | 🔴 骨架 | 网关适配 |
| `architect-token-servlet` | 🔴 骨架 | Servlet 适配 |
| `architect-token-webflux` | 🔴 骨架 | WebFlux 适配 |
| `architect-token-temporary` | 🔴 骨架 | 临时 Token |

### architect-token-support 核心能力

#### Token 生成

`TokenStyle` 枚举，5 种生成风格：
- `uuid`：`UUID.randomUUID()` 带横线
- `simple-uuid`：UUID 去横线
- `random-32`：32 位随机字符串
- `random-64`：64 位随机字符串
- `random-128`：128 位随机字符串

#### 会话管理

| 接口/类 | 功能 |
|---------|------|
| `ISessionRepository` | 会话仓储接口（14 个方法：CRUD + TTL + 会话管理） |
| `MemorySessionRepository` | 内存实现，同时实现 `ISessionRepository`、`IDualSafeRepository`、`IMutedRepository`，惰性过期 + 后台清理 |
| `Session` | 会话模型：realm、loginId、token、createTime、attr、extra |

#### 核心注解

| 注解 | 目标 | 功能 |
|------|------|------|
| `@Permission` | METHOD/TYPE | 权限校验：`realm`、`permit`、`mode`(AND/OR)、`orRole` |
| `@MutedCheck` | METHOD/TYPE | 禁言校验：`realm`、`values`（分组模块）、`level`（封禁等级） |
| `@DoubleCheck` | METHOD/TYPE | 二次密码验证：`value`（业务标识）、`realm` |

#### 配置（`TokenConfig`）

```yaml
com.cloud.token:
  token-name: "Authorization"
  timeout: 7d
  active-timeout: -1        # -1 永不冻结
  concurrent: true          # 多端登录
  max-login-count: 12
  token-style: simple-uuid
  auto-renew: true
  auth-excludes:
    - /public/**
    - /health/**
  error-code:
    auth: 10401
    security: 10403
    muted: 11403
    dual: 11401
```

#### 事件机制

`TokenEvent`（token/realm/loginId）+ `TokenEventListener<E>`（`onHandle`）支持登录/登出/踢下线等事件通知。

### architect-token-redis

`RedisSessionRepository`、`RedisSafeRepository`、`RedisMutedRepository` 使用 Redisson `RBucket` 实现存储，支持 TTL 和批量操作。

---

## 20. architect-idempotent、duplicate、mutex-lock 关系

三个模块构成递进的防重复体系：

| 维度 | architect-duplicate | architect-idempotent | architect-mutex-lock |
|------|--------------------|----------------------|----------------------|
| **关注点** | 数据唯一性（同值不重复） | 请求唯一性（同 Key 不重复执行） | 资源竞争（同资源不同时操作） |
| **粒度** | 数据库字段级 | 方法调用级 | 任意资源级 |
| **机制** | SELECT COUNT + 抛异常 | INSERT IGNORE / SET NX | 乐观锁 / Lua + PubSub |
| **存储** | MySQL | MySQL / Redis | MySQL / Redis |
| **失败策略** | 抛 400 异常 | 抛 429 异常 | 阻塞等待 / 超时 |

---

## 21. architect-aliyun — 阿里云集成

**路径**: `architect-aliyun/`  
**类型**: 2 个子模块

### 子模块

| 子模块 | 功能 |
|--------|------|
| `architect-aliyun-oss` | OSS 对象存储（上传/删除/Web 直传/回调校验） |
| `architect-aliyun-mobile` | 短信发送 + 手机号码一键认证 |

### architect-aliyun-oss

| 核心类 | 功能 |
|--------|------|
| `OssStorageTemplate` | OSS 上传/删除核心，支持 byte[]/File/InputStream 三种上传 |
| `OssPolicyGenerator` | Web 直传 Policy + 签名生成 |
| `UploadCallbackExecutor` | 直传回调 RSA 签名验证（从 OSS 公钥地址拉取密钥） |

### architect-aliyun-mobile

| 核心类 | 功能 |
|--------|------|
| `CloudSmsExecutor` | 短信发送，同步/异步（虚拟线程池） |
| `SmsFlowController` | 短信流控接口（缓存验证码/校验验证码/发送频率），可自定义覆盖 |
| `VerifyMobileExecutor` | 本机号码校验（阿里云号码认证 SDK） |
| `GetMobileExecutor` | 通过 token 换取手机号（一键登录） |

---

## 22. architect-ip2region — IP 定位

**路径**: `architect-ip2region/`  
**类型**: 单模块

基于 ip2region.xdb 离线数据库的 IP ↔ 地理位置查询。

| 核心类 | 功能 |
|--------|------|
| `Ip2RegionSearcher` | 全内存缓冲搜索器（`SmartInitializingSingleton` 自动加载，`DisposableBean` 释放） |
| `IpRegionResult` | 结果 DTO：country/region/province/city/isp，`getAddress()` 自动拼接去重去空 |

---

## 23. architect-search — 搜索引擎（骨架）

**路径**: `architect-search/`  
**状态**: 🔴 骨架模块（仅有 `Main.java` 占位）

---

## 模块成熟度总览

| 状态 | 模块 | 说明 |
|------|------|------|
| ✅ 生产就绪 | architect-bom、commons、spring、redisson、cache、mybatis、aggregate | 核心基础设施 |
| ✅ 生产就绪 | architect-webmvc（全部 7 个子模块） | Web 层全套 |
| ✅ 生产就绪 | architect-event、rocketmq、transaction | 事件/消息/事务 |
| ✅ 生产就绪 | architect-bizlog、operate、duplicate、idempotent、mutex-lock | 安全/日志/锁 |
| ✅ 生产就绪 | architect-aliyun、ip2region | 外部集成 |
| ⚠️ 核心实现 | architect-token | support + redis 已实现，其他适配器骨架 |
| 🔴 骨架 | architect-pulsar、scheduler、search | 待开发 |

---

## 模块间依赖关系

```
architect-bom (版本管理中心)
│
├─ architect-commons (被所有模块依赖)
│
├─ architect-redisson (被 cache / event / token-redis / mutex-lock-redis / idempotent-redis 依赖)
│
├─ architect-cache ───────── 依赖 redisson
├─ architect-event ───────── 依赖 commons + mutex-lock（分布式锁调度）
├─ architect-transaction ──── 依赖 commons + mutex-lock
├─ architect-rocketmq ─────── 依赖 commons
├─ architect-mybatis ──────── 依赖 commons
├─ architect-webmvc ──────── 依赖 commons + redisson（可选）
├─ architect-bizlog ──────── 依赖 commons
├─ architect-operate ──────── 依赖 commons + webmvc-commons + ip2region
├─ architect-idempotent ──── 依赖 commons + redisson（可选）
├─ architect-mutex-lock ──── 依赖 commons + redisson（可选）
├─ architect-duplicate ────── 依赖 commons
├─ architect-token ───────── 依赖 commons + redisson（可选）
├─ architect-aliyun ──────── 依赖 commons
└─ architect-ip2region ───── 无内部依赖
```

---

## 文档信息

- **生成日期**：2026-05-10
- **覆盖模块**：全部 22 个模块
- **代码行数估算**：约 500+ Java 文件，约 50,000+ 行代码
