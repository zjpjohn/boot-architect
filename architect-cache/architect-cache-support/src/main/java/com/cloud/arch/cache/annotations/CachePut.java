package com.cloud.arch.cache.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CachePut {

    String[] names() default "";

    String key() default "";

    String keyGenerator() default "";

    String cacheResolver() default "";

    String condition() default "";

    String unless() default "";

    /**
     * 是否开启L1缓存
     */
    boolean enableLocal() default false;

    /**
     * L1缓存配置信息
     */
    Local local() default @Local();

    /**
     * L2缓存配置信息
     */
    Remote remote() default @Remote();

}
