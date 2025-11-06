package com.cloud.arch.cache.support;


import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

public interface CacheOperationSource {

    /**
     * 获取方法上的缓存操作集合
     *
     * @param targetType 目标执行类
     * @param method     缓存操作方法
     */
    Collection<AbsCacheOperation<? extends Annotation>> getCacheOperations(Class<?> targetType, Method method);

    /**
     * 缓存方法上缓存操作集合
     */
    void cacheOperations(Class<?> targetType, Method method, Set<? extends Annotation> annotations);

    /**
     * 根据缓存操作构建缓存
     */
    void cacheBuild();

}
