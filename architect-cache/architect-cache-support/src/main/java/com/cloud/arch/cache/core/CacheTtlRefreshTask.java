package com.cloud.arch.cache.core;

public interface CacheTtlRefreshTask {

    /**
     * 刷新缓存
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    void refreshTtl(Object key, Object value);

}

