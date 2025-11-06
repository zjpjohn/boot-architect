package com.cloud.arch.annotations;

import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperateLog {

    /**
     * 业务分组
     */
    String group() default "";

    /**
     * 操作业务编号
     */
    String bizNo() default "";

    /**
     * 操作成功模板
     */
    String success() default "";

    /**
     * 租户标识
     */
    String tenant() default "";

    /**
     * 失败操作模板
     */
    String failure() default "";

    /**
     * 操作者
     */
    String operator() default "";

    /**
     * 操作内容详情
     */
    String detail() default "";

    /**
     * 操作过滤条件
     */
    String condition() default "";

}
