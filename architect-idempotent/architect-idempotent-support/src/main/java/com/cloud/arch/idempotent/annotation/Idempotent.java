package com.cloud.arch.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * key前缀
     */
    String prefix() default "";

    /**
     * 幂等key,使用spel进行动态计算key
     */
    String key();

    /**
     * 使用数据库模式时，在分库分表情况下进行分片
     */
    String sharding() default "";

    /**
     * 使用redis做幂等时，锁过期时间
     */
    long expireTime() default 10L;

    /**
     * redis锁过期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * redis作为分布式锁，方法完成是否立即删除
     */
    boolean removeNow() default false;

    /**
     * 自定义消息提示
     */
    String message() default "重复操作，请稍后再试";

}
