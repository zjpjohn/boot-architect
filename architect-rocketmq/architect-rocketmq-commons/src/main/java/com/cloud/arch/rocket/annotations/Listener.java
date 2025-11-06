package com.cloud.arch.rocket.annotations;


import com.cloud.arch.rocket.idempotent.Idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {

    /**
     * 消息topic
     * 支持${}占位符
     */
    String topic() default "";

    /**
     * 消息过滤tag
     * 支持${}占位符
     */
    String tag() default "";

    /**
     * 消息消费幂等
     * NONE-默认非幂等
     * JDBC-数据库校验幂等，不保证事务一致性
     * TRANSACTION-数据库校验保证事务一致性，推荐数据库业务操作时使用
     */
    Idempotent idempotent() default Idempotent.NONE;

}
