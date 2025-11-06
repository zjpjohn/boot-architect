package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.Cache;

public class SimpleCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(Exception exception, Cache cache, Object key) {
        throw new RuntimeException(exception);
    }

    @Override
    public void handleCachePutError(Exception exception, Cache cache, Object key, Object value) {
        throw new RuntimeException(exception);
    }

    @Override
    public void handleCacheEvictError(Exception exception, Cache cache, Object key) {
        throw new RuntimeException(exception);
    }

    @Override
    public void handleCacheClearError(Exception exception, Cache cache) {
        throw new RuntimeException(exception);
    }
}
