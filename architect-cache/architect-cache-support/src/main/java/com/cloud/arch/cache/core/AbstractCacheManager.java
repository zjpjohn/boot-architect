package com.cloud.arch.cache.core;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(64);
    private final Set<String> cacheNames = new CopyOnWriteArraySet<>();

    @Override
    public Cache getCache(String name) {
        return cacheMap.get(name);
    }

    @Override
    public Cache getAndAdd(String name, CacheSettings settings) {
        return this.cacheMap.computeIfAbsent(name, key -> {
            Cache cache = this.getMissingCache(key, settings);
            cache = this.decorateCache(cache);
            this.updateCacheNames(key);
            return cache;
        });
    }

    /**
     * 动态激活本地缓存
     *
     * @param name 缓存名称
     */
    @Override
    public void activateLocal(String name) {

    }

    /**
     * 卸载本地缓存
     *
     * @param name 缓存名称
     */
    @Override
    public void detachLocal(String name) {

    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheNames;
    }

    /**
     * 创建缓存实例
     *
     * @param name 缓存名称
     * @param settings 缓存配置信息
     */
    public abstract Cache getMissingCache(String name, CacheSettings settings);

    protected final Cache lookupCache(String name) {
        return this.cacheMap.get(name);
    }

    private void updateCacheNames(String name) {
        this.cacheNames.add(name);
    }

    protected Cache decorateCache(Cache cache) {
        return cache;
    }

}
