package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface CacheResolver {

    Collection<Cache> resolveCache(AbsCacheOperation<? extends Annotation> operation);

}
