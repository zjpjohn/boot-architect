package com.cloud.arch.cache.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheResult {
    /**
     * the name cache instance
     */
    String[] names() default "";

    /**
     * cache key support SPEL
     */
    String key() default "";

    /**
     * To use key generator.
     * Mutually exclusive with the {@link #key} attribute.
     */
    String keyGenerator() default "";

    /**
     * To use cacheResolver bean name
     */
    String cacheResolver() default "";

    /**
     * cache condition
     */
    String condition() default "";

    /**
     * exclude cache
     */
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
