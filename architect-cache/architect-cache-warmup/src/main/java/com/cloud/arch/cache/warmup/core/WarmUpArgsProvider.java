package com.cloud.arch.cache.warmup.core;

import java.util.List;

@FunctionalInterface
public interface WarmUpArgsProvider {

    /** 返回指定缓存名的预热参数，每组 Object[] 是一次方法调用的入参 */
    List<Object[]> provide(String cacheName);
}
