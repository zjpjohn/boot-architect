package com.cloud.arch.cache.interceptor;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.support.CacheErrorHandler;
import com.cloud.arch.cache.support.CacheEvictManager;
import com.cloud.arch.cache.support.SimpleCacheErrorHandler;

import java.util.Optional;

public abstract class AbstractCacheInvoker {

    protected final CacheErrorHandler errorHandler;
    protected final CacheEvictManager cacheEvictManager;

    public AbstractCacheInvoker(CacheEvictManager cacheEvictManager) {
        this(cacheEvictManager, new SimpleCacheErrorHandler());
    }

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
