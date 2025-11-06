package com.cloud.arch.web.annotation;

import com.cloud.arch.web.support.GrantMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {

    String DEFAULT_VALUE = "*";

    /**
     * 接口请求访问域,访问域-大范围限制接口权限
     */
    String[] domain() default DEFAULT_VALUE;

    /**
     * 权限集合
     * 说明：多个权限用','分隔
     */
    String[] permit() default DEFAULT_VALUE;

    /**
     * 角色集合
     * 说明：多个角色用','分隔
     */
    String[] role() default DEFAULT_VALUE;

    /**
     * 权限校验模式
     * 默认模式-AND
     */
    GrantMode mode() default GrantMode.AND;

}
