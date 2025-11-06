package com.cloud.arch.cache.annotations;


import com.cloud.arch.cache.core.ExpireMode;
import com.cloud.arch.cache.utils.CacheConstants;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Local {

    /**
     * 一级缓存初始缓存容量，默认500
     */
    int initialSize() default CacheConstants.INITIAL_SIZE;

    /**
     * 一级缓存最大允许缓存容量，默认2000
     */
    int maximumSize() default CacheConstants.MAXIMUM_SIZE;

    /**
     * 一级缓存过期时间，默认600秒
     */
    long expire() default CacheConstants.LOCAL_EXPIRE;

    /**
     * 一级缓存过期模式，默认模式:ExpireMode.WRITE
     */
    ExpireMode expireMode() default ExpireMode.WRITE;
}
