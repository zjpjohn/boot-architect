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

    /**
     * 是否允许缓存预热，默认 false。
     * 仅作用于预热扫描器，对 AOP 缓存拦截无影响。
     */
    boolean warmup() default false;

    /**
     * 缓存备注，配合 warmup 使用，说明该缓存的业务场景
     */
    String remark() default "";
}
