package com.cloud.arch.annotation;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RptField {

    /**
     * 重复校验表名
     */
    String table();

    /**
     * 重复校验字段名称
     */
    String column();

    /**
     * 约束字段，多个字段用','分隔，
     * 校验时会严格校验传入的参数值
     * Aop方式不支持传入约束字段，传入约束字段奖校验不通过
     */
    String constraints() default "";

    /**
     * 重复时提示消息
     */
    String message() default "数据已存在";

}
