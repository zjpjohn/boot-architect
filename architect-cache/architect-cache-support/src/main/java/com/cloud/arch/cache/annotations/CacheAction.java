package com.cloud.arch.cache.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAction {

    /**
     * names of default caches to consider for caching operations defined
     */
    String[] names() default "";

    /**
     * the bean name of the default key generator
     */
    String keyGenerator() default "";

    /**
     * the bean name of the custom cache resolver to use
     */
    String cacheResolver() default "";

}
