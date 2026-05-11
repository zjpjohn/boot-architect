# architect-webmvc-security 权限组件使用文档

## 1. 概述

本组件为 Spring Boot 应用提供**多访问域下的角色+权限双维度鉴权**能力，通过两条独立通道协同工作：

- **注解鉴权**：`@Permission` 注解声明在 Controller 类或方法上，由 Spring AOP（`StaticMethodMatcherPointcutAdvisor` + `MethodInterceptor`）拦截
- **URI 配置鉴权**：`application.yml` 中声明 URL 模式与权限规则的映射，由 `HandlerInterceptor` 拦截

两条通道共享 `SecurityPrincipal` SPI 和 `SecurityPrincipalProcessor` 鉴权引擎，决策逻辑完全一致。

### 核心特性

- **多访问域原生隔离**：一个应用同时服务管理后台、小程序、开放 API，各自使用独立的 `SecurityPrincipal` 实现
- **类+方法双层注解**：类级配置为默认值（角色/权限由方法覆盖），域属性类与方法求并集
- **角色+权限双维度校验**：支持 AND（同时满足）和 OR（满足其一）两种组合模式
- **三级缓存**：注解元数据缓存 → URI 匹配缓存 → 鉴权结果缓存（Caffeine TTL）
- **零外部依赖**：鉴权逻辑完全自包含，不依赖 Spring Security

### 适用场景

- 微服务网关后的内部服务鉴权（网关已完成身份认证）
- 多端共用一个后端服务的权限隔离（管理后台、小程序、开放 API 等）
- 不需要 OAuth2/CSRF/Session 管理的轻量级鉴权

### 与 Spring Security 的关系

本组件与 Spring Security **零依赖、可共存**。Spring Security 负责认证（你是谁），本组件负责鉴权（你能做什么）。典型部署：

```
客户端 → 网关（Spring Security 认证 + 注入 header） → 后端服务（本组件读取 header 鉴权）
```

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-webmvc-security</artifactId>
</dependency>
```

> 如果项目已使用 `architect-webmvc-boot-starter`，本模块已自动引入。

### 2.2 实现 SecurityPrincipal

这是**唯一必须的接入步骤**。实现 `SecurityPrincipal` 接口并注册为 Spring Bean：

```java
@Component
public class DefaultSecurityPrincipal implements SecurityPrincipal {

    @Override
    public String domain() {
        return "default";  // 与请求头 Auth-Access-Domain 的值对应
    }

    @Override
    public GrantedPrincipal principal(String identity) {
        // identity = 请求头 Auth-Request-Identity 的值
        // 从数据库/缓存查询该用户的角色和权限集合
        Set<String> roles   = queryRoles(identity);
        Set<String> permits = queryPermits(identity);
        return new GrantedPrincipal(roles, permits);
    }
}
```

### 2.3 标注 @Permission

```java
@RestController
@Permission(domain = "admin")       // 整个 Controller 仅 admin 域可访问
public class UserController {

    @Permission(domain = "open")    // 此接口额外开放给 open 域（域求并集）
    @GetMapping("/user/{id}")
    public ApiReturn<User> getUser(@PathVariable String id) { }

    @Permission(permit = "user:delete", role = "admin")
    @DeleteMapping("/user/{id}")
    public ApiReturn<Void> deleteUser(@PathVariable String id) { }
}
```

---

## 3. 配置项

所有配置以 `com.cloud.web.security` 为前缀：

```yaml
com:
  cloud:
    web:
      security:
        # 是否开启权限校验，默认 true
        enable: true

        # 拦截器拦截的 URL 路径，多个用逗号分隔，默认 /**
        patterns: /api/**,/web/**

        # 排除的 URL 路径，多个用逗号分隔
        excludes: /api/health,/api/public/**

        # 是否开启鉴权结果缓存，默认 false
        cached: true

        # 缓存最大容量，默认 1024
        cache-max-size: 2048

        # 缓存过期时间，默认 5m
        cache-expire: 10m

        # URI 资源配置（详见第 5 节）
        resources:
          - /user/** | post,put,delete | * | role(admin)
          - /job/execute | * | system | permit(job:write)
          - /public/** | get | * | *
```

### 配置项速查

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enable` | boolean | `true` | 全局开关，关闭后整个鉴权组件不装配 |
| `patterns` | String | `/**` | Interceptor 拦截路径，逗号分隔 |
| `excludes` | String | — | Interceptor 排除路径，逗号分隔 |
| `cached` | boolean | `false` | 是否缓存鉴权结果 |
| `cache-max-size` | int | `1024` | Caffeine 缓存最大条目数 |
| `cache-expire` | Duration | `5m` | 缓存过期时间 |
| `resources` | List\<String\> | 空 | URI 资源配置列表，为空时不注册 Interceptor |

> **排除路径说明**：Interceptor 默认已排除静态资源文件（`/**/*.html`, `/**/*.css`, `/**/*.js`, `/**/*.png`, `/**/*.jpg`, `/**/*.jpeg`, `/**/*.JPG`, `/**/*.webp`, `/**/*.ico`）以及 `WebShareProperties.excludes()` 中配置的路径，`excludes` 配置项会与这些默认排除合并。

---

## 4. @Permission 注解

### 4.1 注解定义

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {

    String DEFAULT_VALUE = "*";

    /** 接口请求访问域 */
    String[] domain() default DEFAULT_VALUE;

    /** 权限集合 */
    String[] permit() default DEFAULT_VALUE;

    /** 角色集合 */
    String[] role() default DEFAULT_VALUE;

    /** 权限校验模式，默认 AND */
    GrantMode mode() default GrantMode.AND;
}
```

> **注意**：`domain`、`permit`、`role` 均为 `String[]` 类型，多个值需使用 Java 数组语法 `{"a", "b"}`，**不要**在单个字符串内用逗号分隔（如 `"a,b"`），`valuesExtract` 不会对字符串做逗号分割。URI 配置中的 `role(a,b)` 和 `permit(a,b)` 则由 `parseInfo` 按逗号分割，两者语法不同。

### 4.2 类+方法双层注解的合并规则

合并逻辑由 `AuthorizationMetadata.PermitAuthority` 实现：

| 属性 | 合并策略 | 说明 |
|------|---------|------|
| `domain` | **求并集** | 类配置 + 方法配置 = 两个域都能访问 |
| `role` | **方法覆盖类** | 方法有配置则以方法为准，方法无配置则取类配置 |
| `permit` | **方法覆盖类** | 同上 |
| `mode` | **方法覆盖类** | 同上 |

#### 域合并示例

```java
@Permission(domain = "app")                    // 类级：仅 app 域
public class OrderController {

    @Permission(domain = "admin")              // 方法级：app 和 admin 域都可访问
    @GetMapping("/order/{id}")
    public ApiReturn<Order> getOrder() { }

    @PostMapping("/order")                     // 继承类：仅 app 域
    public ApiReturn<Void> createOrder() { }
}
```

#### 角色/权限覆盖示例

```java
@Permission(role = "user")                     // 类级：默认 user 角色
public class ConfigController {

    @Permission(role = "admin")                // 方法级：覆盖为 admin 角色
    @DeleteMapping("/config/{key}")
    public ApiReturn<Void> deleteConfig() { }

    @GetMapping("/config/{key}")               // 继承类：user 角色
    public ApiReturn<Config> getConfig() { }
}
```

### 4.3 校验模式

鉴权决策核心位于 `GrantAuthority.decide()`，通过 `Sets.intersection` 计算用户具备的角色/权限与注解要求的角色/权限的交集：

```java
// AND 模式（默认）：角色和权限必须同时满足
@Permission(role = "admin", permit = "user:write", mode = GrantMode.AND)

// OR 模式：角色或权限满足其一即可
@Permission(role = "admin", permit = "user:write", mode = GrantMode.OR)

// 仅角色校验
@Permission(role = {"admin", "operator"})

// 仅权限校验
@Permission(permit = {"order:create", "order:view"})

// 仅域校验：域内所有用户可访问
@Permission(domain = "admin")
```

### 4.4 空角色+空权限的含义

当 `role` 和 `permit` 均为 `*`（或不填），表示**仅校验访问域**。此时 `GrantAuthority.isEmpty()` 返回 true，`SecurityPrincipalProcessor` 检测到后直接放行，不调用 `SecurityPrincipal.principal()`。

---

## 5. URI 资源配置

### 5.1 配置格式

```
资源路径 | HTTP方法 | 访问域 | 角色/权限表达式
```

每段用 `|` 分隔，共 4 段（第 4 段可选）：

| 段位 | 含义 | 取值 |
|------|------|------|
| 第 1 段 | URI 路径模式 | Ant 风格路径，如 `/user/**` |
| 第 2 段 | HTTP 方法 | `*`（所有方法）或 `get,post,put,delete` 等 |
| 第 3 段 | 访问域 | `*`（所有域）或 `d1,d2` |
| 第 4 段 | 角色/权限表达式 | 省略或 `*` 表示不校验角色权限；`role(...)` / `permit(...)` 或组合 |

### 5.2 表达式语法

解析逻辑位于 `UriResourceAuthority.parse()` → `parseRoleOrAuthority()`：

| 表达式 | 含义 |
|--------|------|
| `*` 或不填 | 通配，不校验角色和权限 |
| `role(r1,r2)` | 拥有 r1/r2 任意一个角色即可 |
| `permit(p1,p2)` | 拥有 p1/p2 任意一个权限即可 |
| `role(...) and permit(...)` | 角色和权限必须同时满足 |
| `role(...) or permit(...)` | 角色或权限满足其一 |

> `and` / `or` 不区分大小写。表达式顺序支持 `role(x) and permit(y)` 和 `permit(x) and role(y)` 两种写法。

### 5.3 配置示例

```yaml
resources:
  # 仅域校验：admin 域下所有 GET 请求放行
  - /public/** | get | admin | *

  # 仅角色校验：POST/PUT/DELETE 需要 admin 角色，不限制访问域
  - /user/** | post,put,delete | * | role(admin)

  # 仅权限校验：需要 user:create 权限
  - /user/** | post | * | permit(user:create)

  # AND 组合：system 域下需要 admin 角色且拥有 job:write 权限
  - /job/execute | * | system | role(admin) and permit(job:write)

  # OR 组合：system 域下拥有 admin 角色或 job:read 权限即可
  - /job/query | get | system | role(admin) or permit(job:read)

  # 仅域校验，所有用户放行
  - /health | get | monitor
```

---

## 6. SecurityPrincipal 接口

### 6.1 接口定义

```java
public interface SecurityPrincipal {

    /** 鉴权域标识，与请求头 Auth-Access-Domain 一一对应 */
    String domain();

    /** 根据用户标识查询其角色和权限 */
    GrantedPrincipal principal(String identity);
}
```

### 6.2 GrantedPrincipal

```java
public record GrantedPrincipal(Set<String> roles, Set<String> permits) {

    /** 仅角色 */
    public static GrantedPrincipal ofRoles(Set<String> roles);

    /** 仅权限 */
    public static GrantedPrincipal ofPermits(Set<String> permits);

    /** 空角色空权限 */
    public static GrantedPrincipal empty();
}
```

### 6.3 多域实现

框架通过 `SmartInitializingSingleton.afterSingletonsInstantiated()` 自动扫描所有 `SecurityPrincipal` Bean，以 `domain()` 为 key 建立索引。多个实现只需声明不同的 domain：

```java
@Component
public class AdminPrincipal implements SecurityPrincipal {
    public String domain() { return "admin"; }
    public GrantedPrincipal principal(String identity) {
        // 管理后台的 RBAC 模型
    }
}

@Component
public class OpenApiPrincipal implements SecurityPrincipal {
    public String domain() { return "open"; }
    public GrantedPrincipal principal(String identity) {
        // 开放平台的 AK/SK 模型
    }
}
```

> 两个实现**不能返回相同的 `domain()`**，否则 `Maps.uniqueIndex` 会因 key 冲突抛异常。

### 6.4 域无对应 SecurityPrincipal 时的行为

当请求域在系统中找不到对应的 `SecurityPrincipal` Bean 时，**默认放行**。这是有意设计：适用于"仅校验访问域是否在允许范围内，而不关心域内具体角色权限"的场景。

---

## 7. 请求头规范

组件通过两个请求头识别请求归属：

| 请求头 | 常量 | 说明 |
|--------|------|------|
| `Auth-Access-Domain` | `WebTokenConstants.ACCESS_SOURCE_HEADER` | 访问域，用于选择对应的 `SecurityPrincipal` |
| `Auth-Request-Identity` | `WebTokenConstants.AUTH_IDENTITY_HEADER` | 用户标识，传入 `principal(identity)` |

典型部署中，这两个请求头由上游网关在认证阶段注入：

```
客户端 → 网关（认证 + 注入 header） → 后端服务（鉴权组件读取 header）
```

### 未注入 header 时的行为

两个通道均通过 `ErrorHandler.check()` 校验 header 非空，失败时抛出 `ApiBizException`。注解通道中异常向上传播到全局异常处理器；URI 通道中 `UriResourceAuthorizeInterceptor` 的 catch 块区分 `ApiBizException`（保留原始错误码）与其他异常（转为 `AUTH_INTERNAL_ERROR` 500）。

| 缺失 header | 注解鉴权 | URI 配置鉴权 |
|------------|---------|------------|
| `Auth-Access-Domain` 为空 | `isValidDomain()` 返回 false → `AUTHORITY_FORBIDDEN` (403) | `CHANNEL_NULL.check()` 抛 `ApiBizException` → `ex.errReturn()` 保留 (403) |
| `Auth-Request-Identity` 为空 | `requireAuthority()` 中 `AUTH_IDENTITY_NONE.check()` 抛异常 (401) | `AUTH_IDENTITY_NONE.check()` 抛 `ApiBizException` → `ex.errReturn()` 保留 (401) |

---

## 8. 鉴权流程

### 8.1 架构图

```
请求到达
  │
  ├─ AOP 通道（StaticMethodMatcherPointcutAdvisor）
  │   │ 切点：类或方法有 @Permission 注解
  │   │
  │   ▼
  │   AnnotationSecurityHandler.invoke()  [MethodInterceptor]
  │   ├─ AuthorizationMetadataFactory.getAndCreate() → 解析注解 → 元数据缓存
  │   ├─ metadata.isEmptyAuthorization() → true 则直接执行方法
  │   ├─ SecurityPrincipalProcessor.annotationAuthorize()
  │   │   ├─ 域校验 (isValidDomain)
  │   │   ├─ 查找 SecurityPrincipal
  │   │   ├─ 检查鉴权结果缓存
  │   │   ├─ 调用 principal(identity) 获取用户角色/权限
  │   │   ├─ GrantAuthority.decide() 执行 AND/OR 决策
  │   │   └─ 缓存鉴权结果
  │   └─ AUTHORITY_FORBIDDEN.check(result) → 拒绝则抛 ApiBizException
  │
  └─ Interceptor 通道（UriResourceAuthorizeInterceptor）
      │ 条件：resources 配置了 URI 规则 → AuthorityInterceptorConfigurer 注册拦截器
      │
      ▼
      UriResourceAuthorizeInterceptor.preHandle()
      ├─ isAuthAnnotated() → 方法有 @Permission → 跳过（交给 AOP）
      └─ UriSecurityProcessor.authorize()
          ├─ UriAuthorityManager.measureAuthority() → URI 匹配（首次线性扫描 → 缓存）
          │   ├─ exclude 缓存命中 → 返回 null（放行）
          │   └─ 未匹配 → 线性扫描 resources → 匹配后缓存 → 未匹配标记排除
          ├─ CHANNEL_NULL.check(domain) / AUTH_IDENTITY_NONE.check(identity)
          └─ SecurityPrincipalProcessor.uriAuthorize()
      ├─ catch(ApiBizException ex) → ex.errReturn() 保留原始错误码
      └─ catch(Exception) → AUTH_INTERNAL_ERROR (500)
```

### 8.2 两通道的优先级

**注解优先于 URI 配置**。`UriSecurityProcessor.isAuthAnnotated()` 检查方法或类上是否有 `@Permission` 注解，有则 Interceptor 直接放行，由 AOP 通道处理。这意味着：

- 标注了 `@Permission` 的方法，URI 配置对其不生效
- 未标注 `@Permission` 的方法，由 URI 配置控制（如有匹配规则）

---

## 9. 缓存机制

### 9.1 三层缓存

| 层级 | 存储 | 缓存内容 | Key | 生命周期 |
|------|------|---------|-----|---------|
| L1 元数据 | `AuthorizationMetadataFactory.metadataCache` | 注解解析结果 `AuthorizationMetadata` | `AnnotatedElementKey` | 应用生命周期 |
| L2 URI 匹配 | `UriAuthorityManager.cacheAuthorities` + `excludes` | URI → 权限资源映射（含未匹配排除标记） | `AnnotatedElementKey` | 应用生命周期 |
| L3 鉴权结果 | `AuthorizeCacheManager.resultCache` | 鉴权结果 `GrantedResult` | `AuthorizeCacheKey(domain, identity, elementKey)` | `cache-expire` 控制 |

### 9.2 缓存流程

L3 鉴权结果缓存仅在 `cached: true` 时启用（默认关闭）：

```
SecurityPrincipalProcessor.annotationAuthorize()
  ├─ AuthorizeCacheManager.fromCache(cacheKey) → 命中则直接返回
  ├─ 未命中 → 调用 principal(identity) → 执行 decide()
  └─ AuthorizeCacheManager.cacheAuthorize(cacheKey, result) → 写入缓存
```

### 9.3 缓存失效

`AuthorizeCacheManager` 内置了按 identity 清理缓存的能力：

```java
// 用户权限变更后，清理该用户的所有鉴权缓存
cacheManager.invalidate("user-123");
```

可在权限变更的回调/监听器中调用，实现即时生效。

---

## 10. 错误码

| 枚举常量 | HTTP 状态码 | 消息 | 触发场景 |
|---------|------------|------|---------|
| `AUTH_IDENTITY_NONE` | 401 | Auth identity is null. | 请求头 `Auth-Request-Identity` 为空 |
| `CHANNEL_NULL` | 403 | Request domain null, please request domain | 请求头 `Auth-Access-Domain` 为空（URI 鉴权通道） |
| `CHANNEL_ERROR` | 403 | Request domain error | 域配置异常 |
| `CHANNEL_FORBIDDEN` | 403 | Forbidden request domain | 请求域不在注解/配置允许的域范围内 |
| `ROLE_NULL` | 403 | No access privilege | 无角色/权限配置 |
| `ROLE_FORBIDDEN` | 403 | Forbidden request | 角色不满足 |
| `AUTHORITY_FORBIDDEN` | 403 | No access privilege | 鉴权拒绝（注解通道 `ErrorHandler.check(false)` 抛出，URI 通道 `authorize()` 返回 false） |
| `AUTHORITY_PROCESSOR_NONE` | 500 | No authority processor | 未找到鉴权处理器（内部配置错误） |
| `AUTH_INTERNAL_ERROR` | 500 | Authority process internal error. | 非 `ApiBizException` 的内部异常（fail-closed 安全模式） |

---

## 11. 最佳实践

### 11.1 域规划

域是一级隔离单位，建议按业务入口划分：

```
推荐：
  admin   — 管理后台
  applet  — 小程序
  open    — 开放 API
  inner   — 内部服务间调用

不推荐：
  web     — 太模糊，业务含义不明
  frontend / backend — 技术划分，业务无意义
```

### 11.2 注解粒度

- **类上配置** `domain` + `role` 作为该 Controller 的默认安全策略
- **方法上** 仅对"例外"接口做精细化调整（如部分接口放宽域限制）
- 不要在类上什么都不配全靠方法——容易遗漏

### 11.3 URI 配置 vs 注解

| 场景 | 推荐 |
|------|------|
| 通配路径批量管控 | URI 配置 |
| 业务方法精细控制 | `@Permission` 注解 |
| 静态资源/公共接口 | URI 配置 + excludes 排除 |
| 第三方/自动生成的 Controller | URI 配置（无法改源码时） |

### 11.4 性能建议

- **生产环境开启缓存**：`cached: true`，`cache-expire` 设 5~10 分钟
- **cache-max-size 估算**：`并发用户数 × 平均每人访问的接口数`，一般 1024~4096 足够
- **SecurityPrincipal.principal() 内部做好缓存**：不要每次查数据库，可使用 Caffeine 本地缓存或 Redis
- **注解元数据和 URI 匹配缓存是永久的**：仅在应用重启后重建，运行时无开销

### 11.5 安全建议

- 组件采用 **fail-closed** 安全模式：鉴权过程异常时拒绝访问，不会因异常而绕过
- 错误响应仅返回通用消息（如 "No access privilege"），不泄露内部权限配置细节
- 建议定期审计 `resources` 配置和 `@Permission` 注解，确保无遗漏接口

---

## 12. 常见问题

**Q: 未配置 `resources` 时 URI 鉴权是否生效？**

不生效。`resources` 为空时 `AuthorityInterceptorConfigurer` 检测到 patternList 为空，Interceptor 不注册，仅注解鉴权生效。

**Q: 方法上既有注解又在 URI 配置中，以哪个为准？**

注解优先。`UriSecurityProcessor.isAuthAnnotated()` 检测到方法或类上有 `@Permission` 后，Interceptor 直接 return true，交由 AOP 注解通道处理。

**Q: 如何实现"仅校验访问域，不校验角色权限"？**

`role` 和 `permit` 设为 `*` 或不填。此时 `GrantAuthority.isEmpty()` 返回 true，鉴权引擎检测后直接放行。

**Q: 角色和权限的区别是什么？**

语义层面：角色对应"你是谁"（admin、editor），权限对应"你能做什么"（user:create、order:delete）。在组件内部，两者校验逻辑完全一致（都是 `Sets.intersection` 集合交集运算），仅配置表达式的 key 不同（`role()` vs `permit()`）。

**Q: 请求域找不到对应的 SecurityPrincipal 时会怎样？**

默认**放行**。这个设计适用于"仅校验访问域是否在允许范围内，不关心域内具体角色权限"的场景。如果需要在域不存在时拒绝，可以在 `SecurityPrincipal.principal()` 中返回 `GrantedPrincipal.empty()` 并确保角色/权限配置了具体值。

**Q: 注解鉴权和 URI 鉴权可以同时使用吗？**

可以。两者的典型分工：URI 配置处理粗粒度批量规则（如 `/admin/**` 需要 admin 角色），`@Permission` 注解处理细粒度例外（如某个接口额外开放给其它域）。

**Q: 能否接入外部 OAuth2 Provider？**

可以。在 `SecurityPrincipal.principal()` 内部调用 OAuth2 `/userinfo` 端点或自省端点，将返回的 scopes/roles 映射为 `GrantedPrincipal` 即可。
