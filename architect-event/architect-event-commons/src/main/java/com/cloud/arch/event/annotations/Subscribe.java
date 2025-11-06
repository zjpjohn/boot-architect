package com.cloud.arch.event.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Inherited
@Documented
@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Subscribes.class)
public @interface Subscribe {

    /**
     * 事件消费者分组,支持${}占位符
     */
    String group() default "";

    /**
     * 事件消费topic,支持${}占位符
     */
    String name() default "";

    /**
     * 事件过滤标识,支持${}占位符
     */
    String filter() default "*";

    /**
     * 分库分表情况下幂等标识字段名称,指定事件内容的字段名称
     */
    String sharding() default "";

    /**
     * 订阅事件自定义幂等处理key,指定事件内容的字段名称
     * 
     */
    String key() default "";
}
