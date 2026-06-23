# Spring Boot 4 + Spring Cloud Alibaba 2025.1.0.0 升级计划

## 版本对照

| 组件 | 当前版本 | 目标版本 | 风险等级 |
|---|---|---|---|
| **项目 revision** | **1.0** | **2.0** | — |
| Spring Boot | 3.5.5 | **4.0.2** | — |
| Spring Framework | 6.x (内嵌) | **7.0.x** (内嵌) | HIGH |
| Spring Cloud | 2025.0.0 | **2025.1.1** | MEDIUM |
| Spring Cloud Alibaba | 2025.0.0.0 | **2025.1.0.0** | HIGH |
| JDK | 25 | 25 (不变, 最低要求 21) | — |
| Jakarta EE | 9 (推测) | **10** (javax→jakarta 已完成) | LOW |
| Netty | 4.1.77 | **5.x** (Spring Boot 4 内嵌) | MEDIUM |

## 核心变更摘要

1. **Spring Boot 4.0 正式支持虚拟线程**（Project Loom），JDK 21+ 强制
2. **Netty 5.0** — io_uring 支持，API 有 breaking change
3. **GraalVM Native Image** 正式化
4. **Spring Framework 7.0** — API 清理，弃用移除
5. **Spring Cloud Alibaba 2025.1.0.0** — Nacos 3.0 / Sentinel 2.0 / Dubbo 4.0 / Seata 2.0

---

## 第三方依赖风险矩阵

| 依赖 | 当前版本 | 目标版本 | Spring Boot 4 适配状态 | 风险 |
|---|---|---|---|---|
| Redisson | 4.5.0 | 4.5.0 (不变) | 已确认兼容 | LOW |
| MyBatis Spring Boot | 3.0.5 | 待确认 | **待确认** | **HIGH** |
| MyBatis-Plus | 3.5.12 | 待确认 | **待确认** | **HIGH** |
| MyBatis-Flex | 1.11.5 | 待确认 | **待确认** | **HIGH** |
| Dubbo | 3.2.19 | 4.0.0 | 已发布 | MEDIUM |
| Druid | 1.2.27 | 待确认 | **待确认** | MEDIUM |
| FastJSON2 | 2.0.61 | 2.0.61 (不变) | 需确认 extension-spring7 | MEDIUM |
| SpringDoc | 2.6.0 | 待确认 | 待确认 | LOW |
| Knife4j | 4.5.0 | 待确认 | **待确认** | MEDIUM |
| Elasticsearch | 8.3.2 | 8.17.x | 与 Spring Boot 无关 | LOW |
| Netty | 4.1.77 | 5.0.0 | Spring Boot 4 内嵌管理 | MEDIUM |
| Lombok | 1.18.42 | 最新 | 需确认 Java 25 兼容 | LOW |
| MapStruct | 1.6.3 | 最新 | 需确认 Java 25 兼容 | LOW |
| Nacos | (BOM 管理) | 3.0.0 | Spring Cloud Alibaba 2025.1.0.0 内置 | MEDIUM |
| Sentinel | (BOM 管理) | 2.0.0 | 虚拟线程感知熔断 | MEDIUM |

---

## 模块升级清单（按升级顺序）

### Phase 1: BOM 基础层（必须先改）

#### 1.1 根 pom.xml — revision + 根配置

**文件**: `pom.xml`

**改动**:

```xml
<properties>
    <revision>2.0</revision>  <!-- 1.0→2.0, 大版本升级 -->
    <!-- 其他属性不变 -->
</properties>
```

#### 1.2 architect-bom — 版本管理中心

**文件**: `architect-bom/pom.xml`

**改动**:

```xml
<properties>
    <!-- 核心框架 -->
    <spring-boot.version>4.0.2</spring-boot.version>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
    
    <!-- 需同步升级的第三方 -->
    <redisson.version>4.5.0</redisson.version>         <!-- 确认兼容，无需改动 -->
    <dubbo.version>4.0.0</dubbo.version>                <!-- 3.2.19→4.0.0, Spring Boot 4 适配 -->
    <mybatis-spring-boot.version>待确认</mybatis-spring-boot.version>  <!-- 3.0.5→?, 待发布 -->
    <mybatis-plus.version>待确认</mybatis-plus.version>  <!-- 3.5.12→?, 待发布 -->
    <mybatis-flex.version>待确认</mybatis-flex.version>  <!-- 1.11.5→?, 待确认 -->
    <druid.version>待确认</druid.version>                <!-- 1.2.27→?, 待确认 -->
    <fastjson2.version>2.0.61</fastjson2.version>        <!-- 确认兼容，可能需 extension-spring7 -->
    <springdoc.version>待确认</springdoc.version>         <!-- 2.6.0→?, Spring Boot 4 适配 -->
    <knife4j.version>待确认</knife4j.version>             <!-- 4.5.0→?, 待确认 -->
    <elasticsearch.version>8.17.0</elasticsearch.version> <!-- 8.3.2→8.17.x, 可选升级 -->
    <lombok.version>1.18.42</lombok.version>             <!-- 确认 Java 25 兼容 -->
    <mapstruct.version>1.6.3</mapstruct.version>          <!-- 确认 Java 25 兼容 -->
    <!-- netty 版本交由 Spring Boot 管理，删除显式声明的 4.1.77 -->
</properties>
```

**风险**: 部分第三方库的 Spring Boot 4 适配版尚未确认。

**验证**: `mvn -pl architect-bom validate` 通过，所有版本号可解析。

---

### Phase 2: 零依赖/低风险模块（可并行）

#### 2.1 architect-commons

**文件**: `architect-commons/pom.xml`

**改动**: 无版本硬编码，依赖皆由 BOM 管理。检查 Spring Framework 7 下 `spring-core` 是否有 API 废弃。

**风险**: LOW。纯工具类，不涉及 Spring Boot AutoConfiguration。

**验证**: `mvn -pl architect-commons compile test`

#### 2.2 architect-spring

**文件**: `architect-spring/pom.xml`

**改动**: 检查 `spring-context` 在 Framework 7 下的兼容性。

**风险**: LOW

**验证**: `mvn -pl architect-spring compile`

#### 2.3 architect-ip2region

**文件**: `architect-ip2region/pom.xml`

**改动**: 无。独立库，不依赖 Spring Boot 版本。

**风险**: LOW

#### 2.4 architect-search

**文件**: `architect-search/pom.xml`（空 POM，无依赖）

**改动**: 无。

**风险**: NONE

#### 2.5 architect-scheduler 所有子模块

**文件**: `architect-scheduler/*/pom.xml`（空 POM，无依赖声明）

**改动**: 无。

**风险**: NONE

#### 2.6 architect-pulsar

**文件**: `architect-pulsar/*/pom.xml`（空 POM）

**改动**: 无。

**风险**: NONE

---

### Phase 3: 中间件集成模块（需逐个验证）

#### 3.1 architect-redisson

**文件**: `architect-redisson/pom.xml`

**当前依赖**: redisson 4.5.0, fastjson2, jackson-datatype-jsr310, spring-boot-starter(optional)

**改动**:
- Redisson 4.5.0 已确认兼容 Spring Boot 4（2026-05 有新发布）
- 检查 `spring-boot-autoconfigure` 是否需要适配 Spring Boot 4 的 AutoConfiguration 机制变更
- 检查 `RScript` API 在 Redisson 是否有变动

**风险**: MEDIUM。Redisson 是多个模块的底层依赖（cache, mutex, idempotent）。

**验证**: `mvn -pl architect-redisson compile`

#### 3.2 architect-mutex-lock

**文件**: `architect-mutex-lock/*/pom.xml`

**改动**:
- `mutex-lock-support` — 纯 Java，无 Spring 依赖，LOW 风险
- `mutex-lock-starter` — 检查 `spring-boot-autoconfigure` 适配
- `mutex-lock-mysql` — 检查 `spring-jdbc` 兼容性
- `mutex-lock-redis` — 依赖 architect-redisson，等 Phase 3.1 完成后验证

**风险**: LOW-MEDIUM

**验证**: `mvn -pl architect-mutex-lock -am compile`

#### 3.3 architect-mybatis

**文件**: `architect-mybatis/*/pom.xml`

**改动**:
- `mybatis-spring-boot-starter`: 3.0.5 → **需等待 Spring Boot 4 适配版**
- `mybatis-plus-spring-boot3-starter`: 3.5.12 → **需等待适配版**（"boot3" 命名暗示仅 Spring Boot 3）
- `mybatis-flex-spring-boot3-starter`: 1.11.5 → **需等待适配版**

**风险**: **HIGH**。最大阻塞点。MyBatis 生态对 Spring Boot 大版本升级的响应速度通常较慢。

**备选方案**:
1. 检查 MyBatis-Plus/MyBatis-Flex 是否有 Spring Boot 4 milestone 版本
2. 如短期无适配，可暂用 `spring-boot-starter-jdbc` + 手动配置 `SqlSessionFactory` 绕过 starter
3. 联系 MyBatis 社区了解发布计划

**验证**: 等适配版发布后再编译验证

#### 3.4 architect-rocketmq

**文件**: `architect-rocketmq/*/pom.xml`

**改动**:
- RocketMQ client 版本由 Spring Cloud Alibaba BOM 管理
- `rocketmq-v5x-starter` — 检查 `rocketmq-client` 5.x 与 Spring Boot 4 兼容性
- `rocketmq-ons-starter` — Aliyun ONS 客户端独立，风险较低

**风险**: MEDIUM

**验证**: `mvn -pl architect-rocketmq -am compile`

#### 3.5 architect-aliyun

**文件**: `architect-aliyun/*/pom.xml`

**改动**: Aliyun SDK 与 Spring Boot 版本无关，无需改动。

**风险**: LOW

**验证**: `mvn -pl architect-aliyun -am compile`

---

### Phase 4: 业务框架模块（高风险）

#### 4.1 architect-cache

**文件**: `architect-cache/*/pom.xml`

**改动**:
- `cache-support` — 检查 Caffeine、Micrometer 版本（由 Spring Boot BOM 管理，自动升级）
- `cache-boot-starter` — 检查 `spring-boot-autoconfigure` 适配
- `cache-hotkey` — 独立组件，风险低
- `cache-warmup` — 检查 `spring-boot-autoconfigure`

**风险**: MEDIUM。Micrometer API 在 Spring Boot 4 中可能有变化。

**验证**: `mvn -pl architect-cache -am compile`

#### 4.2 architect-event

**文件**: `architect-event/*/pom.xml`

**改动**:
- `event-commons` — micrometer-core
- `event-boot-starter` — 检查 autoconfigure 适配
- `event-queue-*` — Kafka/RocketMQ/RabbitMQ/Pulsar 客户端版本
- `event-storage-jdbc` — 检查 `spring-jdbc` 兼容性
- `event-storage-rocksdb` — 独立，低风险

**风险**: MEDIUM。队列客户端多，需逐一验证。

**验证**: `mvn -pl architect-event -am compile`

#### 4.3 architect-webmvc

**文件**: `architect-webmvc/*/pom.xml`

**改动**:
- `webmvc-commons` — `spring-web`, `jakarta.validation-api` → Jakarta EE 10
- `webmvc-jackson` — Jackson 版本由 Spring Boot BOM 管理
- `webmvc-fastjson2` — **检查 `fastjson2-extension-spring6` 是否需升级到 extension-spring7**
- `webmvc-security` — Spring Security 版本由 BOM 管理
- `webmvc-webtoken-gateway` — `spring-cloud-starter-gateway` 升级到 2025.1.1
- `webmvc-swagger` — SpringDoc OpenAPI → 新版本
- `webmvc-boot-starter` — autoconfigure 适配

**风险**: **HIGH**。涉及 Spring MVC、Spring Security、Spring Cloud Gateway 多个核心组件。
- `fastjson2-extension-spring6` 可能需改为 `extension-spring7`
- SpringDoc 新版本 API 可能有变化
- Gateway 路由配置可能有 breaking change

**验证**: `mvn -pl architect-webmvc -am compile`

#### 4.4 architect-idempotent

**文件**: `architect-idempotent/*/pom.xml`

**改动**: Spring AOP + Aspects，检查 Spring Framework 7 下的 AOP 兼容性。

**风险**: LOW-MEDIUM

**验证**: `mvn -pl architect-idempotent -am compile`

#### 4.5 architect-bizlog

**文件**: `architect-bizlog/*/pom.xml`

**改动**:
- `bizlog-mysql` — spring-jdbc 兼容
- `bizlog-mongodb` — `spring-boot-starter-data-mongodb` 版本由 BOM 管理
- `bizlog-elasticsearch` — ES 客户端 8.3.2，与 Spring Boot 无关

**风险**: LOW

**验证**: `mvn -pl architect-bizlog -am compile`

#### 4.6 architect-transaction

**文件**: `architect-transaction/*/pom.xml`

**改动**: Spring TX + AOP + Aspects，检查 Framework 7 兼容性。

**风险**: MEDIUM。事务管理是核心功能。

**验证**: `mvn -pl architect-transaction -am compile`

#### 4.7 architect-token

**文件**: `architect-token/*/pom.xml`

**改动**: JWT 库独立，WebFlux 子模块需检查 Spring WebFlux 变化。

**风险**: LOW

**验证**: `mvn -pl architect-token -am compile`

#### 4.8 architect-operate

**文件**: `architect-operate/*/pom.xml`

**改动**:
- `operate-endpoint` — `spring-boot-starter-actuator` 在 Boot 4 中 Actuator 端点 API 有变化
- `operate-spring-starter` — autoconfigure 适配

**风险**: MEDIUM。Actuator API 是主要关注点。

**验证**: `mvn -pl architect-operate -am compile`

#### 4.9 architect-duplicate

**文件**: `architect-duplicate/pom.xml`

**改动**: Spring AOP + Aspects + JDBC，低风险。

**风险**: LOW

#### 4.10 architect-aggregate

**文件**: `architect-aggregate/pom.xml`

**改动**: Fory Core 独立，无 Spring Boot 依赖。

**风险**: LOW

---

### Phase 5: 示例模块

#### 5.1 architect-xmodules

**文件**: `architect-xmodules/*/pom.xml`

**改动**: 示例应用，随主模块升级自动适配。需逐个启动验证功能。

**风险**: LOW（不发布到 Maven，仅内部验证）

---

## 关键风险 & 阻塞项

1. **MyBatis 生态**（最大阻塞点）
   - mybatis-spring-boot-starter / mybatis-plus / mybatis-flex 的 Spring Boot 4 适配版发布时间不确定
   - 备选方案：手动配置 SqlSessionFactory 绕过 starter
2. **FastJSON2 extension**
   - `fastjson2-extension-spring6` 需改为 extension-spring7，否则 WebMVC 消息转换器可能不工作
3. **Netty 版本冲突**
   - 项目显式声明 `4.1.77`，Spring Boot 4 内嵌 Netty 5.x
   - 建议去掉 BOM 中的 netty 版本声明，全交给 Spring Boot 管理
4. **Druid**
   - `druid-spring-boot-3-starter:1.2.27` "boot-3" 命名暗示可能不兼容 Spring Boot 4
5. **Spring Cloud Gateway**
   - 2025.1.1 版本可能有路由配置 breaking change

---

## 推荐升级步骤

1. **环境准备** — JDK 25 已就绪，创建 `feature/upgrade-spring-boot4` 分支
2. **BOM 先行** — 修改 architect-bom 版本号，单独提交
3. **零依赖模块** — 并行升级 commons, spring, ip2region, search, scheduler, pulsar
4. **中间件模块** — 按序：redisson → mutex-lock → rocketmq → aliyun
5. **核心框架模块** — 按序：cache → event → webmvc → idempotent → bizlog → transaction → token → operate → duplicate → aggregate
6. **MyBatis 生态**（阻塞项）— 等适配版发布后升级
7. **示例验证** — 启动 xmodules 示例应用，端到端验证

---

## 验证计划

1. 每个 Phase 完成后执行 `mvn -pl <module> -am compile`，确保编译通过
2. 全部模块升级后执行 `mvn clean install -DskipTests`，确保整个项目可安装
3. 执行 `mvn test` 验证测试套件
4. 启动 cache-example, event-example, web-example 验证核心场景
