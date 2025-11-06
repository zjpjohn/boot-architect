package com.cloud.arch.operate.annotations;


import com.cloud.arch.operate.core.OperateType;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /**
     * 业务分组
     */
    String bizGroup() default "";

    /**
     * 操作类型
     */
    OperateType type();

    /**
     * 操作说明
     */
    String title() default "";

    /**
     * 排除过滤掉敏感参数
     */
    String[] excludes() default "";

}
