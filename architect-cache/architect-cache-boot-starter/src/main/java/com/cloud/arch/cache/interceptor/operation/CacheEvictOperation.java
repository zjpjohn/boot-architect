package com.cloud.arch.cache.interceptor.operation;


import com.cloud.arch.cache.annotations.CacheAction;
import com.cloud.arch.cache.annotations.CacheEvict;

import java.lang.reflect.Method;

public class CacheEvictOperation extends AbsCacheOperation<CacheEvict> {

    private boolean allEntries;
    private boolean beforeInvocation;

    public CacheEvictOperation(Method method, CacheEvict annotation, CacheAction cacheAction) {
        super(method, annotation, cacheAction);
    }

    @Override
    void parse(Method method, CacheEvict annotation) {
        this.setCacheNames(annotation.names());
        this.setCacheResolver(annotation.cacheResolver());
        this.setKey(annotation.key());
        this.setKeyGenerator(annotation.keyGenerator());
        this.setCondition(annotation.condition());
        this.setAllEntries(annotation.allEntries());
        this.setBeforeInvocation(annotation.beforeInvocation());
    }

    public boolean isAllEntries() {
        return allEntries;
    }

    public void setAllEntries(boolean allEntries) {
        this.allEntries = allEntries;
    }

    public boolean isBeforeInvocation() {
        return beforeInvocation;
    }

    public void setBeforeInvocation(boolean beforeInvocation) {
        this.beforeInvocation = beforeInvocation;
    }
}
