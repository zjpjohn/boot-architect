package com.cloud.arch.rocket.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Delay {

    /**
     * 延迟时间模式:
     * true-延迟到指定时间(未来某个精确时间点)
     * false-延迟指定时间段(从当前时间算到未来一段时间，例如：延迟30分钟),时间单位是毫秒
     */
    boolean deliver() default false;

    /**
     * 延迟时间段时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
}
