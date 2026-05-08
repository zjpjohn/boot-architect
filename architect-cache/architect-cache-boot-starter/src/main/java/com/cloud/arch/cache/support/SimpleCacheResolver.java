package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * 默认缓存解析器，直接从 {@link AbsCacheOperation#getCacheNames()} 获取缓存名称并解析为 Cache 实例
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

    public SimpleCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Collection<String> getCacheNames(AbsCacheOperation<? extends Annotation> operation) {
        return operation.getCacheNames();
    }

}
