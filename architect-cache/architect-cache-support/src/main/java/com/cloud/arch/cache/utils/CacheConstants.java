package com.cloud.arch.cache.utils;

public class CacheConstants {
    /**
     * 本地缓存初始容量默认值:500
     */
    public static final int  INITIAL_SIZE  = 512;
    /**
     * 本地缓存最大容量默认值:2000
     */
    public static final int  MAXIMUM_SIZE  = 2048;
    /**
     * 本地缓存默认过期时间:600秒
     */
    public static final long LOCAL_EXPIRE  = 600;
    /**
     * 二级缓存默认过期时间:1800秒
     * 缓存过期时间:expire+random
     */
    public static final long REMOTE_EXPIRE = 1800;
    /**
     * 缓存刷新提前时间:缓存过期时间前300秒刷新缓存
     */
    public static final long PRELOAD_TIME  = 300;
    /**
     * 二级缓存null值过期时间比率:3,expire/magnification
     */
    public static final int  MAGNIFICATION = 3;
    /**
     * 缓存过期随机时间(0~1200)
     */
    public static final int  RANDOM_BOUND  = 1200;

}
