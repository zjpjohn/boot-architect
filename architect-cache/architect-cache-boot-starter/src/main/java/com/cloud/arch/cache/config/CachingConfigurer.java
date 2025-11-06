package com.cloud.arch.cache.config;


import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.support.*;

public interface CachingConfigurer {

    /**
     * return cache manager
     * if you custom cache manager,can be used
     */
    CacheManager cacheManager();

    /**
     * create or build cache resolver
     */
    CacheResolver cacheResolver();

    /**
     * create or build key generator,default key generator is SimpleKeyGenerator
     */
    default KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    /**
     * create cache error handler,default error handler is SimpleCacheErrorHandler
     */
    default CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
}
