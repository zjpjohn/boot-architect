package com.cloud.arch.event.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Documented
@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Publishes.class)
public @interface Publish {

    /**
     * 发布事件业务分组名称
     * 通过业务分组能够使快速定位事件使用场景
     */
    String bizGroup() default "";

    /**
     * 1.name为空表示为本地事件
     * 2.name非空表示远程跨应用事件,支持${}占位符
     * name具体示意：
     * kafka-队列topic
     * rocketmq-队列topic,
     * rabbitmq-队列routeKey/queueName
     */
    String name() default "";

    /**
     * rocketmq/ons消息过滤tag,支持${}占位符
     */
    String filter() default "";

    /**
     * 延迟事件：rabbitmq,rocketmq-ons,rocketmq-v5.0,pulsar支持延迟事件，具体使用请参考各个消息队列延迟消息
     * 延迟事件延迟时间
     * 0-表示不延迟,默认不启用延迟事件
     * >0-表示延迟时间
     */
    long delay() default 0L;

    /**
     * 延迟事件延迟时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
