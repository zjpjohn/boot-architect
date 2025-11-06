package com.cloud.arch.rocket.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sender {

    /**
     * 消息队列主题
     * 支持${}占位符
     */
    String topic() default "";

    /**
     * 消息过滤tag
     * 支持${}占位符
     */
    String tag() default "";

    /**
     * 发送消息超时时间
     * 默认：3000毫秒
     */
    int timeout() default 3000;

    /**
     * 是否为延迟消息
     */
    boolean delay() default false;

    /**
     * 批量发送消息
     * <p>
     * 最多支持一次发送100条消息
     */
    boolean batch() default false;

    /**
     * 默认批量发送100条，超过数量分批发送
     */
    int batchSize() default 100;

    /**
     * 是否顺序发送消息
     */
    boolean orderly() default false;

    /**
     * 是否异步发送消息
     * <p>
     * 对于响应时间敏感的业务场景，无需等待broker响应
     * </p>
     */
    boolean async() default false;

    /**
     * 单向模式发送消息
     * <p>
     * 无需关心发送结果的场景，如发送日志消息
     * </p>
     */
    boolean oneWay() default false;

}
