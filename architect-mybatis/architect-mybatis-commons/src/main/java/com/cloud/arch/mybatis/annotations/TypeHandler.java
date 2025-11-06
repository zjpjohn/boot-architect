package com.cloud.arch.mybatis.annotations;

import com.cloud.arch.mybatis.core.Type;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

/**
 * 标记在需要使用的枚举或JSON对象类上
 */
@Inherited
@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TypeHandler {

    /**
     * 字段类型，目前支持两种类型['JSON','ENUM']
     */
    Type type();

}
