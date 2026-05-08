# architect-webmvc-security 权限组件使用文档

## 1. 概述

本组件为 Spring Boot 应用提供**多访问域下的角色+权限双维度鉴权**能力。支持两条独立的鉴权通道：

- **URI 配置鉴权**：基于 URL 模式和 HTTP 方法的声明式配置，由 Interceptor 实现
- **注解鉴权**：基于 `@Permission` 注解的方法级声明式鉴权，由 Spring AOP 实现

两条通道共享同一个 `SecurityPrincipal` SPI，鉴权决策逻辑完全一致。

### 核心特性

- 多访问域原生隔离：一个应用同时服务多个前端/网关，各自使用独立的鉴权实现
- 类+方法双层注解：类级配置为默认值，方法级精确覆盖
- 角色 + 权限双维度校验，支持 AND/OR 组合模式
- 三级缓存：方法元数据缓存 → URI 匹配缓存 → 鉴权结果缓存
- 零外部依赖：鉴权逻辑完全自包含，不依赖 Spring Security

### 适用场景

- 微服务网关后的内部服务鉴权
- 多租户 SaaS 应用的多域权限隔离
- 不需要 OAuth2/CSRF/Session 管理的轻量级鉴权需求

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.cloud.arch</groupId>
    <artifactId>architect-webmvc-security</artifactId>
</dependency>
```

> 如果你的项目使用 `architect-webmvc-boot-starter`，已自动引入本模块。

### 2.2 实现 SecurityPrincipal

这是**唯一必须的接入步骤**。实现 `SecurityPrincipal` 接口并注册为 Spring Bean：

```java
@Component
public class DefaultSecurityPrincipal implements SecurityPrincipal {

    @Override
    public String domain() {
        return "default";  // 访问域标识，与请求头 Auth-Access-Domain 对应
    }

    @Override
    public GrantedPrincipal principal(String identity) {
        // identity = 请求头 Auth-Request-Identity 的值
        // 从数据库/缓存查询该用户的角色和权限
        Set<String> roles   = queryRolesByIdentity(identity);
        Set<String> permits = queryPermitsByIdentity(identity);
        return new GrantedPrincipal(roles, permits);
    }
}
```

#### 多域场景

```java
@Component("adminPrincipal")
public class AdminSecurityPrincipal implements SecurityPrincipal {
    @Override
    public String domain() { return "admin"; }

    @Override
    public GrantedPrincipal principal(String identity) {
        // 管理后台的 RBAC 模型
    }
}

@Component("openApiPrincipal")
public class OpenApiSecurityPrincipal implements SecurityPrincipal {
    @Override
    public String domain() { return "open"; }

    @Override
    public GrantedPrincipal principal(String identity) {
        // 开放平台的 AK/SK 模型
    }
}
```

### 2.3 标注 @Permission

```java
@RestController
@Permission(domain = "admin")       // 整个 Controller 仅 admin 域可访问
public class UserController {

    @Permission(domain = "open")    // 此接口额外开放给 open 域
    @GetMapping("/user/{id}")
    public ApiReturn<User> getUser(@PathVariable String id) {
        // ...
    }

    @Permission(permit = "user:delete", role = "admin")  // 角色+权限双校验
    @DeleteMapping("/user/{id}")
    public ApiReturn<Void> deleteUser(@PathVariable String id) {
        // ...
    }
}
```

---

## 3. 配置项

所有配置以 `com.cloud.web.security` 为前缀，在 `application.yml` 中配置：

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
        # 默认已包含静态资源路径：/static/**,/public/**,/resources/**,/templates/**,/META-INF/resources/**
        excludes: /api/health,/api/public/**

        # 是否开启鉴权结果缓存，默认 false
        cached: true

        # 缓存最大容量，默认 1024
        cache-max-size: 2048

        # 缓存过期时间（分钟），默认 5
        cache-expire: 10m

        # 未知访问域处理策略：false=拒绝（默认），true=放行
        unknown-domain: false

        # URI 资源配置（详见第 4 节）
        resources:
          - /user/** | post,put,delete | * | role(admin)
          - /job/execute | * | system | permit(job:write)
          - /public/** | get | * | *
```

### 配置项速查

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enable` | boolean | `true` | 全局开关，关闭后所有鉴权失效 |
| `patterns` | String | `/**` | Interceptor 拦截路径 |
| `excludes` | String | — | Interceptor 排除路径 |
| `cached` | boolean | `false` | 是否缓存鉴权结果 |
| `cache-max-size` | int | `1024` | Caffeine 缓存最大条目数 |
| `cache-expire` | Duration | `5m` | 缓存过期时间 |
| `unknown-domain` | boolean | `false` | 未知访问域放行策略 |
| `resources` | List\<String\> | 空 | URI 资源配置列表 |

---

## 4. @Permission 注解

### 4.1 注解定义

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {

    String DEFAULT_VALUE = "*";

    /** 访问域，默认 * 表示所有域 */
    String[] domain() default DEFAULT_VALUE;

    /** 权限集合 */
    String[] permit() default DEFAULT_VALUE;

    /** 角色集合 */
    String[] role() default DEFAULT_VALUE;

    /** 校验模式：AND（默认）或 OR */
    GrantMode mode() default GrantMode.AND;
}
```

### 4.2 类 + 方法双层注解的合并规则

| 属性 | 合并策略 | 说明 |
|------|---------|------|
| `domain` | **求并集** | 类上配 + 方法上配 = 两个域都能访问。类配方法不配 = 继承类配置。类不配方法配 = 仅方法域 |
| `role` / `permit` | **方法覆盖类** | 方法有注解 → 以方法为准；方法无注解 → 以类为准 |
| `mode` | **方法覆盖类** | 同上 |

#### 示例：域合并

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

#### 示例：角色/权限覆盖

```java
@Permission(role = "user")                     // 类级：默认 user 角色可访问
public class ConfigController {

    @Permission(role = "admin")                // 方法级：覆盖为 admin 角色
    @DeleteMapping("/config/{key}")
    public ApiReturn<Void> deleteConfig() { }

    @GetMapping("/config/{key}")               // 继承类：user 角色可访问
    public ApiReturn<Config> getConfig() { }
}
```

### 4.3 校验模式

```java
// AND 模式（默认）：角色和权限必须同时满足
@Permission(role = "admin", permit = "user:write", mode = GrantMode.AND)

// OR 模式：角色或权限满足其一即可
@Permission(role = "admin", permit = "user:write", mode = GrantMode.OR)

// 仅角色校验
@Permission(role = "admin,operator")           // 拥有 admin 或 operator 角色之一

// 仅权限校验
@Permission(permit = "order:create,order:view") // 拥有任意一个权限即可

// 仅域校验，不限制角色和权限
@Permission(domain = "admin")
```

#### 空角色+空权限的含义

当 `@Permission` 仅配置了 `domain` 而未配置 `role` 和 `permit`（或两者都为 `*`），表示**仅校验访问域**，该域下的所有用户均可访问。

---

## 5. URI 资源配置

### 5.1 配置格式

```
资源路径 | HTTP方法 | 访问域 | 角色/权限表达式
```

每段用 `|` 分隔，共 4 段：

| 段位 | 含义 | 取值 |
|------|------|------|
| 第 1 段 | URI 路径模式 | Ant 风格路径，如 `/user/**` |
| 第 2 段 | HTTP 方法 | `*` 或 `get,post,put,delete` 等 |
| 第 3 段 | 访问域 | `*` 或 `d1,d2,d3` |
| 第 4 段 | 角色/权限表达式 | `*`、`role(...)`、`permit(...)` 或组合 |

### 5.2 配置示例

```yaml
resources:
  # 仅域校验：所有 GET 请求在 admin 域下放行
  - /public/** | get | admin | *

  # 仅角色校验：POST/PUT/DELETE 需要 admin 角色
  - /user/** | post,put,delete | * | role(admin)

  # 仅权限校验：需要 user:create 权限
  - /user/** | post | * | permit(user:create)

  # AND 组合：需要 admin 角色且拥有 job:write 权限
  - /job/execute | * | system | role(admin) and permit(job:write)

  # OR 组合：拥有 admin 角色或 job:read 权限即可
  - /job/query | get | system | role(admin) or permit(job:read)

  # 仅域校验，所有用户放行
  - /health | get | monitor | *
```

### 5.3 第 4 段表达式语法

| 表达式 | 含义 |
|--------|------|
| `*` | 通配，不校验角色和权限 |
| `role(r1,r2,r3)` | 拥有 r1/r2/r3 任意一个角色即可 |
| `permit(p1,p2,p3)` | 拥有 p1/p2/p3 任意一个权限即可 |
| `role(...) and permit(...)` | 角色和权限必须同时满足 |
| `role(...) or permit(...)` | 角色或权限满足其一 |

> `and` / `or` 不区分大小写，`AND` 与 `and` 等效。

---

## 6. SecurityPrincipal 接口

### 6.1 接口定义

```java
public interface SecurityPrincipal {

    /** 鉴权域标识，需与请求头 Auth-Access-Domain 一致 */
    String domain();

    /** 根据用户标识查询角色和权限 */
    GrantedPrincipal principal(String identity);
}
```

### 6.2 返回类型

```java
public record GrantedPrincipal(Set<String> roles, Set<String> permits) {

    /** 仅角色 */
    public static GrantedPrincipal ofRoles(Set<String> roles);

    /** 仅权限 */
    public static GrantedPrincipal ofPermits(Set<String> permits);

    /** 空 */
    public static GrantedPrincipal empty();
}
```

### 6.3 实现范例

```java
@Component
public class MySecurityPrincipal implements SecurityPrincipal {

    private final UserRoleService userRoleService;

    public MySecurityPrincipal(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @Override
    public String domain() {
        return "my-app";
    }

    @Override
    public GrantedPrincipal principal(String identity) {
        UserPermissions up = userRoleService.getByUserId(identity);
        if (up == null) {
            return GrantedPrincipal.empty();
        }
        return new GrantedPrincipal(up.getRoles(), up.getPermits());
    }
}
```

### 6.4 多实现注册

当有多个 `SecurityPrincipal` Bean 时，框架以 `domain()` 为 key 自动建立索引。多个实现注册不同的 domain 即可：

```java
@Component public class AdminPrincipal implements SecurityPrincipal {
    public String domain() { return "admin"; }
    // ...
}

@Component public class AppletPrincipal implements SecurityPrincipal {
    public String domain() { return "applet"; }
    // ...
}
```

> 注意：两个实现不能返回相同的 `domain()`，否则启动时会因索引冲突失败。

---

## 7. 请求头规范

组件通过两个请求头识别请求的归属域和用户身份：

| 请求头 | 常量 | 说明 |
|--------|------|------|
| `Auth-Access-Domain` | `WebTokenConstants.ACCESS_SOURCE_HEADER` | 访问域标识，用于选择对应的 `SecurityPrincipal` 实现 |
| `Auth-Request-Identity` | `WebTokenConstants.AUTH_IDENTITY_HEADER` | 用户身份标识，传入 `principal(identity)` 方法 |

这两个请求头通常由上游网关或 webtoken 组件在认证阶段注入：

```
客户端 → 网关（认证 + 注入 header） → 后端服务（鉴权组件读取 header）
```

### 未注入 header 时的行为

- `Auth-Access-Domain` 为空 → 域校验失败，返回 403
- `Auth-Request-Identity` 为空 → 身份校验失败，返回 401
- `Auth-Access-Domain` 对应的 `SecurityPrincipal` 未注册 → 根据 `unknown-domain` 配置决定

---

## 8. 鉴权流程详解

### 8.1 注解鉴权流程

```
请求到达 Controller 方法
  ↓
AOP Advisor 拦截（类或方法有 @Permission）
  ↓
AuthorizationMetadataFactory 解析注解元数据（首次缓存）
  ↓
SecurityPrincipalProcessor.annotationAuthorize()
  ├─ 1. 读取 Auth-Access-Domain 请求头
  ├─ 2. 域校验：是否在注解允许的域范围内
  ├─ 3. 查找对应的 SecurityPrincipal 实现
  ├─ 4. 检查缓存（如有）
  ├─ 5. 调用 SecurityPrincipal.principal(identity) 获取用户角色/权限
  ├─ 6. 根据 GrantMode 执行 AND/OR 决策
  └─ 7. 缓存结果（如开启）
  ↓
通过 → 执行 Controller 方法
拒绝 → 抛出 IllegalStateException
```

### 8.2 URI 配置鉴权流程

```
请求到达
  ↓
UriResourceAuthorizeInterceptor.preHandle()
  ├─ 检查 HandlerMethod 是否标注 @Permission（有则跳过，交由 AOP 处理）
  ├─ 调用 UriAuthorityManager.measureAuthority() 匹配 URI 资源
  │   ├─ 首次：线性扫描所有 resources，匹配后缓存
  │   └─ 后续：直接从缓存获取
  ├─ 读取 Auth-Access-Domain 和 Auth-Request-Identity 请求头
  └─ 调用 SecurityPrincipalProcessor.uriAuthorize() 执行鉴权
  ↓
通过 → 继续执行
拒绝 → 返回 JSON 错误响应
```

> 注解鉴权优先级高于 URI 配置鉴权：当方法标注了 `@Permission`，URI 配置对该方法不生效。

---

## 9. 缓存机制

### 三层缓存架构

| 层级 | 存储 | 缓存内容 | 生命周期 |
|------|------|---------|---------|
| L1 元数据 | `AuthorizationMetadataFactory` | 注解解析结果（`AuthorizationMetadata`） | 应用生命周期 |
| L2 URI 匹配 | `UriAuthorityManager` | URI → 权限资源映射 | 应用生命周期 |
| L3 鉴权结果 | `AuthorizeCacheManager` | `(domain, identity, method)` → 鉴权结果 | 由 `cache-expire` 控制 |

### 缓存配置建议

```yaml
com.cloud.web.security:
  cached: true              # 生产环境建议开启
  cache-max-size: 2048      # 根据 (用户数 × 平均访问接口数) 估算
  cache-expire: 5m          # 权限变更容忍窗口
```

### 缓存失效

当前缓存依赖 TTL 自动过期。如需立即失效某个用户的缓存（如权限变更后），可通过以下方式扩展：

```java
// AuthorizeCacheManager 中增加失效方法
public void invalidate(String identity) {
    if (resultCache != null) {
        resultCache.asMap().keySet()
            .removeIf(key -> key.identity().equals(identity));
    }
}
```

---

## 10. 错误码

| 枚举常量 | HTTP 状态码 | 消息 | 触发条件 |
|---------|------------|------|---------|
| `AUTH_IDENTITY_NONE` | 401 | Auth identity is null. | 请求头 `Auth-Request-Identity` 为空 |
| `CHANNEL_NULL` | 403 | Request domain null, please request domain | 请求头 `Auth-Access-Domain` 为空 |
| `CHANNEL_ERROR` | 403 | Request domain error | 域配置错误 |
| `CHANNEL_FORBIDDEN` | 403 | Forbidden request domain | 请求域不在允许范围内 |
| `ROLE_NULL` | 403 | No access privilege | 无角色/权限配置 |
| `ROLE_FORBIDDEN` | 403 | Forbidden request | 角色不足 |
| `AUTHORITY_FORBIDDEN` | 403 | No access privilege | 权限不足（注解鉴权） |
| `AUTHORITY_PROCESSOR_NONE` | 500 | No authority processor | 未找到鉴权处理器 |
| `AUTH_INTERNAL_ERROR` | 500 | Authority process internal error. | 鉴权过程内部异常 |

---

## 11. 最佳实践

### 11.1 域规划

域是组件的一级隔离单位。建议按业务入口划分，而非按技术层级：

```
✅ 推荐：
  admin   — 管理后台
  applet  — 小程序
  open    — 开放 API
  inner   — 内部服务调用

❌ 不推荐：
  web     — 太模糊
  frontend, backend — 技术划分，业务无意义
```

### 11.2 注解粒度

- 类上配 `domain` + `role` 作为该 Controller 的默认安全策略
- 方法上仅对"例外"做精细化调整
- 不要在类上什么都不配全靠方法——容易遗漏

### 11.3 URI 配置 vs 注解的选择

| 场景 | 推荐方式 |
|------|---------|
| 网关层粗粒度拦截 | URI 配置 |
| 业务方法精细控制 | `@Permission` 注解 |
| 通配路径批量管控 | URI 配置 |
| 特定接口特殊处理 | `@Permission` 注解 |
| 静态资源/公共接口 | URI 配置 + excludes 排除 |

### 11.4 性能建议

- 生产环境开启缓存（`cached: true`）
- `cache-max-size` 设为 `用户数 × 2`（一个用户通常频繁访问的接口在 10 个以内）
- `cache-expire` 设为 5~10 分钟，权限变更低频场景足够
- `SecurityPrincipal.principal()` 内部做好数据缓存，不要每次查数据库

---

## 12. 常见问题

**Q: 未配置 `resources` 时 URI 鉴权是否生效？**

不生效。`resources` 为空时 Interceptor 不会注册，仅注解鉴权生效。

**Q: 方法上既有注解又在 URI 配置中，以哪个为准？**

注解优先。`UriSecurityProcessor.isAuthAnnotated()` 检测到方法有 `@Permission` 注解后，URI 鉴权直接放行，交由 AOP 注解鉴权处理。

**Q: 如何实现"仅校验访问域，不校验角色权限"？**

`role` 和 `permit` 设为 `*` 或不填：

```java
@Permission(domain = "admin")  // role 和 permit 默认为 *，不校验
```

**Q: 角色和权限的区别是什么？**

语义上：角色对应"你是谁"（如 admin、editor），权限对应"你能做什么"（如 user:create、order:delete）。在组件内部两者校验逻辑完全一致（都是 Set 交集运算），仅配置表达式的 key 不同。

**Q: 能否接入外部 OAuth2 Provider？**

可以实现 `SecurityPrincipal`，在 `principal()` 内部调用 OAuth2 `/userinfo` 端点或内省端点即可。
