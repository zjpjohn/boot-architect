package com.cloud.arch.cache.interceptor;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.support.CacheErrorHandler;
import com.cloud.arch.cache.support.CacheEvictManager;
import com.cloud.arch.cache.support.SimpleCacheErrorHandler;

import java.util.Optional;

public abstract class AbstractCacheInvoker {

    protected final CacheErrorHandler errorHandler;
    protected final CacheEvictManager cacheEvictManager;

    /**
     * 使用默认的 SimpleCacheErrorHandler 构造，缓存操作异常会抛出 RuntimeException
     */
    public AbstractCacheInvoker(CacheEvictManager cacheEvictManager) {
        this(cacheEvictManager, new SimpleCacheErrorHandler());
    }

    /**
     * @param cacheEvictManager 缓存淘汰管理器
     * @param errorHandler      自定义缓存错误处理器，可为 null（此时异常静默）
     */
    public AbstractCacheInvoker(CacheEvictManager cacheEvictManager, CacheErrorHandler errorHandler) {
        this.errorHandler      = errorHandler;
        this.cacheEvictManager = cacheEvictManager;
    }

    protected Object doCacheResult(Cache cache, Object key) {
        try {
            return cache.get(key);
        } catch (Exception error) {
            if (errorHandler != null) {
                errorHandler.handleCacheGetError(error, cache, key);
            }
        }
        return null;
    }

    protected void doCachePut(Cache cache, Object key, Object value) {
        try {
            Optional.ofNullable(cache).ifPresent(v -> v.put(key, value));
        } catch (Exception error) {
            if (errorHandler != null) {
                errorHandler.handleCachePutError(error, cache, key, value);
            }
        }
    }

}
