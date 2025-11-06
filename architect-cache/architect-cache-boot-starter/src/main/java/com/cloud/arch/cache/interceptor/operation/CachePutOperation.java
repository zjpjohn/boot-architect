package com.cloud.arch.cache.interceptor.operation;


import com.cloud.arch.cache.annotations.CacheAction;
import com.cloud.arch.cache.annotations.CachePut;
import com.cloud.arch.cache.core.CacheSettings;

import java.lang.reflect.Method;

public class CachePutOperation extends AbsCacheOperation<CachePut> {

    private String        unless;
    private CacheSettings settings;

    public CachePutOperation(Method method, boolean allowNullValue, CachePut annotation, CacheAction cacheAction) {
        super(method, allowNullValue, annotation, cacheAction);
    }

    @Override
    void parse(Method method, CachePut annotation) {
        this.setCacheNames(annotation.names());
        this.setCacheResolver(annotation.cacheResolver());
        this.setKey(annotation.key());
        this.setKeyGenerator(annotation.keyGenerator());
        this.setCondition(annotation.condition());
        this.setUnless(annotation.unless());
        this.settings
                = CacheSettings.build(annotation.enableLocal(), this.isAllowNullValue(), annotation.remote(), annotation.local());
    }

    @Override
    public boolean canBuildCache() {
        return true;
    }

    public String getUnless() {
        return unless;
    }

    public void setUnless(String unless) {
        this.unless = unless;
    }

    public CacheSettings getSettings() {
        return settings;
    }

}
