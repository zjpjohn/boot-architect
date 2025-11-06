package com.cloud.arch.cache.core;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
@SuppressWarnings("unchecked")
public abstract class AbstractRemoteCache extends AbstractValueAdaptCache {

    /* 远程缓存配置 */
    private final CacheSettings settings;
    /* 是否激活本地缓存 */
    private boolean activatedLocal = false;
    /* 持有的本地缓存 */
    private AbstractLocalCache localCache;

    protected AbstractRemoteCache(String name, CacheSettings settings) {
        super(name, settings.isAllowNullValue());
        this.settings = settings;
    }

    /**
     * get value from second cache
     *
     * @param key cache key of value
     */
    public abstract Object doGet(Object key);

    /**
     * get value from second cache, if cache value null and load value with loader
     *
     * @param key cache key of value
     * @param valueLoader value loader
     */
    public abstract Object doGet(Object key, Callable<?> valueLoader);

    /**
     * put value to second cache with key
     *
     * @param key cache key of value
     * @param value cache value
     */
    public abstract void doPut(Object key, Object value);

    /**
     * evict second cache key of value
     *
     * @param key cache key
     */
    public abstract void doEvict(Object key);

    /**
     * clear all second cache value
     */
    public abstract void doClear();

    /**
     * get cache settings
     */
    public CacheSettings getSettings() {
        return settings;
    }

    /**
     * remote cache contain local cache maybe null if not activate local
     */
    public AbstractLocalCache getLocalCache() {
        return localCache;
    }

    /**
     * 是否已激活本地缓存
     */
    public boolean isActivatedLocal() {
        return activatedLocal;
    }

    /**
     * 激活L2缓存的L1缓存
     *
     * @param localCache L1缓存实例
     */
    public synchronized void activateLocal(AbstractLocalCache localCache) {
        if (!activatedLocal) {
            this.localCache = localCache;
            this.activatedLocal = true;
        }
    }

    /**
     * 卸载L2缓存的L1缓存
     */
    public synchronized void detachLocal() {
        if (this.activatedLocal) {
            this.activatedLocal = false;
            this.localCache.doClear();
            this.localCache = null;
        }
    }

    /**
     * L2、L1缓存查询
     *
     * @param key 缓存key
     */
    @Override
    public <T> T get(Object key) {
        if (this.isActivatedLocal()) {
            return localCache.get(key);
        }
        Object storeValue = doGet(key);
        return (T)toValue(storeValue);
    }

    /**
     * L2、L1缓存查询
     *
     * @param key 缓存key
     * @param valueLoader 缓存值加载器
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (this.isActivatedLocal()) {
            return localCache.get(key, valueLoader);
        }
        Object storeValue = doGet(key, valueLoader);
        return (T)toValue(storeValue);
    }

    /**
     * 更新L2、L1缓存
     *
     * @param key 缓存key
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        if (this.isActivatedLocal()) {
            localCache.put(key, value);
            return;
        }
        Object storeValue = toStoreValue(value);
        doPut(key, storeValue);
    }

    /**
     * 删除指定缓存值
     *
     * @param key 缓存key
     */
    @Override
    public void evict(Object key) {
        if (this.isActivatedLocal()) {
            localCache.evict(key);
            return;
        }
        doEvict(key);
    }

    /**
     * 清空当前缓存
     */
    @Override
    public void clear() {
        if (this.isActivatedLocal()) {
            localCache.clear();
            return;
        }
        doClear();
    }

}
