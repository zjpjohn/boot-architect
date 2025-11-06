package com.cloud.arch.web.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiBody {

    /**
     * 是否加密相应内容，AES加密方式加密
     */
    boolean encrypt() default false;

    /**
     * 自定义响应消息内容
     */
    String message() default "";

}
