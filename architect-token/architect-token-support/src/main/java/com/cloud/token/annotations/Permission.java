package com.cloud.token.annotations;

import com.cloud.token.security.SecureMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限校验
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {

    /**
     * 多账号体系下所属账户领域
     */
    String realm() default "";

    /**
     * 目标方法所需权限集合
     */
    String[] permit() default {};

    /**
     * 权限校验模式
     * 默认-AND模式
     */
    SecureMode mode() default SecureMode.AND;

    /**
     * 权限校验不通过时，进行角色校验
     * 备注:
     * orRole={"admin"，"user"，"agent"} 表示三个角色满足其一即可
     * orRole={"admin,user,agent"} 表示三个角色必须全部满足
     */
    String[] orRole() default {};

}
