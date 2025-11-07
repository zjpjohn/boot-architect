package com.cloud.arch.web.dict;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Inherited
@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Dictionary {

    /**
     * 字典标识名称：应用内不允许重复
     */
    String name();

    /**
     * 字典数据类型说明，如:int,string,double,float等,根据实际数据类型填写
     */
    String type() default "";

    /**
     * 字典数据说明
     */
    String remark() default "";

}
