package com.cloud.arch.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ApiVersion {
    /**
     * 接口版本
     * 接口版本规则:x.x或x.x.x,每部分必须为整数
     * 版本示例:1.0,1.0.1,1.20.12
     */
    String value() default "";

}
