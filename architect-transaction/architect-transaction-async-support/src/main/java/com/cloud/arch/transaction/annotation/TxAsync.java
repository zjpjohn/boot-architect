package com.cloud.arch.transaction.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TxAsync {

    /**
     * 异步任务名称
     */
    String name() default "";

    /**
     * 当前事务任务版本
     * 此参数主要目的因接口业务变动修改参数，可设置版本控制
     * 版本格式示例：1.0 , 1.1
     */
    String version() default "1.0";

    /**
     * 重试时间间隔基数，按指数增长:30,60,120,240,480,960...
     * 注：重试间隔时间基数不小于10秒
     */
    long retryInterval() default 30L;

    /**
     * 异步任务最大重试次数：默认8次
     */
    int maxRetry() default 8;

}
