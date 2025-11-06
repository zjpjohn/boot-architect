# boot-architect

> java应用开发中间件集合，经过多年沉淀，已形成领域驱动、领域事件(
> 抽象统一各个消息队列：AliYunOns、rabbitmq4/5、rocketmq、pulsar,kafka已实现)、二级缓存、
> rocketmq消息队列统一封装、springmvc统一规范、 自实现简单易用的权限框架、异步事务以及幂等校验
> 等中间件

# 项目结构说明

>
> architect-aggregate: 领域驱动聚合，采用snapshot快照模式实现
>
> architect-aliyun: 阿里云oss，阿里云一键登录、短信发送封装
>
> architect-bizlog: 注解实现业务操作日志
>
> architect-bom: 整体依赖BOM
>
> architect-cache: 二级缓存中间件、集成hotkey自动发现热点数据，并对hotkey进行二开
>
> architect-commons: 公共支撑工具包
>
> architect-duplicate: 业务字段重复校验
>
> architect-event: 领域事件封装，并对消息队列进行同意抽象，已接入rabbitmq、kafka、rocketmq4/5、AliYunOns以及pulsar消息对接
>
> architect-idempotent: 幂等校验中间件
>
> architect-ip2region: ip转换地区封装
>
> architect-mutex-lock: 互斥锁，已实现数据库和redis，未来接入hazelcast和ignite
>
> architect-mybatis: mybatis/mybatis-plus扩展，封装enum,json读写
>
> architect-operate: 系统操作日志记录以及查询封装
>
> architect-pulsar: pulsar消息队列封装，时间原因暂未实现
>
> architect-redisson: redisson封装
>
> architect-rocketmq: rocketmq消息队列统一封装，简化在业务系统中的使用（rocketmq5.x暂未实现）
>
> architect-scheduler: 自研分布式调度系统(旧版本暂未迁移过来，后续有时间再迁移过来)
>
> architect-search: elasticsearch封装，目标通过注解编译期代码生成解放繁琐的查询条件编写(旧版本暂未迁移过来)
>
> architect-spring: spring封装以及业务扩展点封装
>
> architect-token: 应用系统授权token管理，暂未实现
>
> architect-transaction: 通过注解异步事物实现封装
>
> architect-webmvc: springmvc统一封装，统一返回结构，异常处理，返回结果加密、接口版本号等实用功能
>
> architect-xmodules: 中间件使用案例，时间有限暂未每个中间件都编写使用例子