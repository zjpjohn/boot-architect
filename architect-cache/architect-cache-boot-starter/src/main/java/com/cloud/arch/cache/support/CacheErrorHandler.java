package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.Cache;
import org.springframework.lang.Nullable;

public interface CacheErrorHandler {

    void handleCacheGetError(Exception exception, Cache cache, Object key);

    void handleCachePutError(Exception exception, Cache cache, Object key, @Nullable Object value);

    void handleCacheEvictError(Exception exception, Cache cache, Object key);

    void handleCacheClearError(Exception exception, Cache cache);

}
