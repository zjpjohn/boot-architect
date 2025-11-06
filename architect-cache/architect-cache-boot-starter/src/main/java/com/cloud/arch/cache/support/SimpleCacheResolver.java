package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class SimpleCacheResolver extends AbstractCacheResolver {

    public SimpleCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Collection<String> getCacheNames(AbsCacheOperation<? extends Annotation> operation) {
        return operation.getCacheNames();
    }

}
