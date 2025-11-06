package com.cloud.arch.cache.interceptor.context;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

public interface CacheContext {


    /**
     * Return the cache operation.
     */
    AbsCacheOperation<? extends Annotation> getOperation();

    /**
     * Return the target instance on which the method was invoked.
     */
    Object getTarget();

    /**
     * Return the method which was invoked.
     */
    Method getMethod();

    /**
     * Return the argument list used to invoke the method.
     */
    Object[] getArgs();

    /**
     * current operation context caches
     */
    Collection<Cache> caches();

}
