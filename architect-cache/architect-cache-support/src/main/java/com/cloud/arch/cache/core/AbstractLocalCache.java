package com.cloud.arch.cache.core;

import com.cloud.arch.utils.SingleFlight;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 1.L1缓存不会独立于L2缓存存在
 * 2.如果L2缓存激活L1缓存，L2缓存的所有操作代理到L1操作
 * 3.集群环境中，需要RefreshPolicy来清除淘汰缓存
 */
@Slf4j
@SuppressWarnings("unchecked")
public abstract class AbstractLocalCache extends AbstractValueAdaptCache {

    private final   SingleFlight<Object, Object> sf = new SingleFlight<>();
    protected final LocalCacheSettings           settings;
    protected final AbstractRemoteCache          remoteCache;
    protected final RefreshPolicy                refreshPolicy;

    protected AbstractLocalCache(String name, boolean allowNullValue, LocalCacheSettings settings, RefreshPolicy refreshPolicy, AbstractRemoteCache remoteCache) {
        super(name, allowNullValue);
        this.settings = settings;
        this.refreshPolicy = refreshPolicy;
        this.remoteCache = remoteCache;
    }

    /**
     * get local cache value with key
     *
     * @param key cache key
     */
    public abstract Object doGet(Object key);

    /**
     * put local cache value with key
     *
     * @param key   cache key
     * @param value cache value
     */
    public abstract void doPut(Object key, Object value);

    /**
     * evict local cache value with specific key
     *
     * @param key cache key
     */
    public abstract void doEvict(Object key);

    /**
     * clear all local cache
     */
    public abstract void doClear();

    @Override
    public <T> T get(Object key) {
        Object value = this.doGet(key);
        if (value != null) {
            this.remoteCache.statsCounter().recordHits(1, true);
            return (T) toValue(value);
        }
        try {
            return (T) sf.execute(new LockKey(key, 0), () -> {
                Object v = this.doGet(key);
                if (v != null) {
                    this.remoteCache.statsCounter().recordHits(1, true);
                    return toValue(v);
                }
                v = remoteCache.doGet(key);
                if (v != null) {
                    this.doPut(key, v);
                }
                return toValue(v);
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = this.doGet(key);
        if (value != null) {
            this.remoteCache.statsCounter().recordHits(1, true);
            return (T) toValue(value);
        }
        try {
            return (T) sf.execute(new LockKey(key, 1), () -> {
                Object v = this.doGet(key);
                if (v != null) {
                    this.remoteCache.statsCounter().recordHits(1, true);
                    return toValue(v);
                }
                v = this.remoteCache.doGet(key, valueLoader);
                if (v != null) {
                    this.doPut(key, v);
                }
                return toValue(v);
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新缓存
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        Object storeValue = toStoreValue(value);
        if (storeValue == null) {
            return;
        }
        try {
            this.remoteCache.doPut(key, storeValue);
            // L2 写入成功，本节点直接更新 L1，避免下次 get 穿透 L1
            this.doPut(key, storeValue);
        } catch (Exception error) {
            log.warn("put cache[{}] key[{}] cached value error:", this.getName(), key, error);
            // L2 写入失败，淘汰本节点 L1 防止返回脏数据
            this.doEvict(key);
        }
        // 无论成败，通知其他节点淘汰 L1，确保集群一致性
        this.refreshPolicy.sendEvict(this.getName(), key);
    }

    /**
     * 清除指定缓存值
     *
     * @param key 缓存key
     */
    @Override
    public void evict(Object key) {
        try {
            this.remoteCache.doEvict(key);
        } catch (Exception error) {
            log.warn("evict cache[{}] key[{}] cached value error:", this.getName(), key, error);
        }
        this.doEvict(key);
        this.refreshPolicy.sendEvict(this.getName(), key);
    }

    /**
     * 清空当前缓存
     */
    @Override
    public void clear() {
        try {
            this.remoteCache.doClear();
        } catch (Exception error) {
            log.warn("clear cache[{}] all cached value error:", this.getName(), error);
        }
        this.doClear();
        this.refreshPolicy.sendClear(this.getName());
    }

    private record LockKey(Object key, int method) {
    }

}
