# boot-architect

> java应用开发中间件集合，经过多年沉淀，已形成领域驱动、领域事件(
> 抽象统一各消息队列：AliYunOns、rabbitmq、rocketmq4/5、pulsar、kafka已接入实现)、二级缓存、
> rocketmq消息队列统一封装、springmvc统一规范、 自实现简洁易用的权限框架、异步事务以及幂等校验
> 等中间件。本中间件已将依赖sdk升级到最新版本，jdk25、fory0.13.0、fastjson2.0.59、spring boot3.5.5、
> spring cloud 2025.0.0、spring cloud alibaba2025.0.0.0

## 项目结构说明

>
> architect-aggregate: 领域驱动聚合操作封装，采用snapshot快照模式实现，内置高性能fory序列化与反序列化
>
> architect-aliyun: 阿里云oss文件上传，手机号一键登录与校验、短信发送接口封装
>
> architect-bizlog: 注解实现业务操作日志，以及操作日志存储统一抽象封装(已实现mysql、mongodb、elasticsearch存储)
>
> architect-bom: 中间件统一依赖管理BOM
>
> architect-cache: 二级缓存中间件，暴露缓存命中指标便于接入告警监控、集成hotkey自动发现热点数据，并对hotkey进行二开
>
> architect-commons: 公共支撑工具包
>
> architect-duplicate: 参数业务字段重复存在校验
>
> architect-event: 领域事件封装，并对消息队列进行统一抽象，已接入本地事件、rabbitmq、kafka、rocketmq4/5、AliYunOns以及pulsar等方式
>
> architect-idempotent: 幂等校验中间件，统一抽象幂等数据存储(已接入mysql、redis，未来接入hazelcast和ignite等方式)
>
> architect-ip2region: ip转换地区封装
>
> architect-mutex-lock: 互斥锁，已实现数据库和redis，未来接入hazelcast和ignite等更多方式
>
> architect-mybatis: mybatis/mybatis-plus扩展，封装enum,json读写
>
> architect-operate: 业务系统操作日志记录以及web端日志查询封装
>
> architect-pulsar: pulsar消息队列封装，个人时间原因暂未实现
>
> architect-redisson: 对redis客户端redisson进行封装，加入fastjson2编解码器
>
> architect-rocketmq: rocketmq消息队列统一封装，简化在业务系统中的使用（rocketmq5.x暂未实现）
>
> architect-scheduler: 自研分布式调度系统(旧版本暂未迁移过来，后续有时间再迁移过来)
>
> architect-search: elasticsearch封装，目标通过注解编译期代码生成解放繁琐的查询条件编写(旧版本暂未迁移过来)
>
> architect-spring: spring封装以及多策略业务执行扩展点封装
>
> architect-token: 应用系统授权token管理，暂未实现
>
> architect-transaction: 通过注解异步事物实现封装，通过@TxAsync即可接入异步事务提高系统吞吐量
>
> architect-webmvc: web返回值简化操作，统一返回结构，统一异常处理，返回结果加密、接口版本号、枚举字典及简洁高效的权限框架等实用功能
>
> architect-xmodules: 中间件使用案例，时间有限暂未每个中间件都编写使用例子