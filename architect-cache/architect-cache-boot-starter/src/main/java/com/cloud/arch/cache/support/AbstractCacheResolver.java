package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.core.CacheSettings;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import com.cloud.arch.cache.interceptor.operation.CachePutOperation;
import com.cloud.arch.cache.interceptor.operation.CacheResultOperation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public abstract class AbstractCacheResolver implements CacheResolver, InitializingBean {

    private final CacheManager cacheManager;

    protected AbstractCacheResolver(CacheManager cacheManager) {
        Assert.notNull(cacheManager, "CacheManager must not be null.");
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    public Collection<Cache> resolveCache(AbsCacheOperation<? extends Annotation> operation) {
        if (!operation.canBuildCache()) {
            return Collections.emptyList();
        }
        Collection<String> cacheNames = this.getCacheNames(operation);
        if (CollectionUtils.isEmpty(cacheNames)) {
            return Collections.emptyList();
        }
        CacheSettings settings = getCacheSettings(operation);
        if (settings == null) {
            // 如果CacheSettings为空，一定是先调用@CacheEvict
            return Collections.emptyList();
        }
        return cacheNames.stream().map(name -> this.cacheManager.getAndAdd(name, settings))
                         .collect(Collectors.toList());
    }

    private static CacheSettings getCacheSettings(AbsCacheOperation<? extends Annotation> operation) {
        CacheSettings settings = null;
        if (operation instanceof CacheResultOperation) {
            CacheResultOperation cacheOperation = (CacheResultOperation) operation;
            settings = cacheOperation.getSettings();
        } else if (operation instanceof CachePutOperation) {
            CachePutOperation cacheOperation = (CachePutOperation) operation;
            settings = cacheOperation.getSettings();
        }
        // 注意@CacheEvict CacheSettings为空
        return settings;
    }

    protected abstract Collection<String> getCacheNames(AbsCacheOperation<? extends Annotation> operation);

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.cacheManager, "CacheManager is required.");
    }
}
