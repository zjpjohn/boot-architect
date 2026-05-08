package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * 缓存解析器，从缓存操作元数据中解析出对应的 {@link com.cloud.arch.cache.core.Cache} 实例集合
 */
public interface CacheResolver {

    Collection<Cache> resolveCache(AbsCacheOperation<? extends Annotation> operation);

}
