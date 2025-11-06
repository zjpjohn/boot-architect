package com.cloud.token.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口禁言校验
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MutedCheck {

    /**
     * 多账号体系下所属账户领域
     */
    String realm() default "";

    /**
     * 禁言分组模块名称
     */
    String values() default "";

    /**
     * 封禁等级
     */
    int level() default -1;

}
