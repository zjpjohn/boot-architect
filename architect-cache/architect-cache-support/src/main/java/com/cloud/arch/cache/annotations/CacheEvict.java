package com.cloud.arch.cache.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

    String[] names() default "";

    String key() default "";

    String keyGenerator() default "";

    String cacheResolver() default "";

    String condition() default "";

    /**
     * 清除全部缓存数据
     * 设置的key或keyGenerator将不起作用
     */
    boolean allEntries() default false;

    /**
     * 是否方法调用前执行缓存清除
     */
    boolean beforeInvocation() default false;

}
