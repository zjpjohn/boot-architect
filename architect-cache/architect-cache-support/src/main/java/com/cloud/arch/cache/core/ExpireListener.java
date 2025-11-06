package com.cloud.arch.cache.core;

public interface ExpireListener {

    /**
     * 一级缓存失效后触发通知
     *
     * @param name 缓存名称
     * @param key  缓存key
     */
    void expireListener(String name, Object key);

}
