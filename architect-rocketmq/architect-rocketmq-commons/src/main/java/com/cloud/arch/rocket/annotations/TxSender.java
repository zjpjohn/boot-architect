package com.cloud.arch.rocket.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TxSender {
    /**
     * 消息topic
     * 支持${}占位符
     */
    String topic() default "";

    /**
     * 消息tag
     * 支持${}占位符
     * 推荐tag使用具有明确业务意义的字符串，避免使用通配符*
     */
    String tag() default "*";

}
