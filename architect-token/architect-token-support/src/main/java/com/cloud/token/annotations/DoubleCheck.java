package com.cloud.token.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Api接口二次输入密码校验
 * 对一些操作敏感的场景使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DoubleCheck {
    /**
     * 业务标识
     */
    String value() default "";

    /**
     * 多账号体系下所属账户领域
     */
    String realm() default "";

}
