package com.cloud.arch.cache.annotations;


import com.cloud.arch.cache.utils.CacheConstants;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Remote {

    /**
     * 缓存过期固定时间,缓存实际过期时间 固定时间expire+随机时间(0~randomBound)
     */
    long expire() default CacheConstants.REMOTE_EXPIRE;

    /**
     * 缓存过期随机时间范围上限,随机时间(0~randomBound)
     */
    int randomBound() default CacheConstants.RANDOM_BOUND;

    /**
     * null值缓存时间比例，null缓存时间expire/magnification,默认比例3
     */
    int magnification() default CacheConstants.MAGNIFICATION;

    /**
     * 在允许刷新二级缓存过期时间时，刷新缓存距离过期时间前preloadTime秒刷新
     */
    long preloadTime() default CacheConstants.PRELOAD_TIME;

    /**
     * 是否允许刷新二级缓存过期时间
     */
    boolean enableRefresh() default false;

}
