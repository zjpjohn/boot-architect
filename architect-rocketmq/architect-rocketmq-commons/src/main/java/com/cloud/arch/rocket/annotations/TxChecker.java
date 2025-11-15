package com.cloud.arch.rocket.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事务消息状态回查器
 * 1.实现TransactionChecker接口
 * 2.标注@TxChecker注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface TxChecker {
    /**
     * 消息topic
     * 支持${}占位符
     */
    String topic() default "";

    /**
     * 消息tag
     * 支持${}占位符
     */
    String tag() default "";

}
