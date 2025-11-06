package com.cloud.arch.cache.core;

import java.util.Objects;
import java.util.concurrent.Callable;

public interface Cache {

    /**
     * 缓存对象的名称.
     */
    String getName();

    /**
     * 缓存的实际实现对象
     */
    Cache getCache();

    <T> T get(Object key);

    <T> T get(Object key, Class<T> type);

    <T> T get(Object key, Callable<T> valueLoader);

    void put(Object key, Object value);

    void evict(Object key);

    void clear();

    long cacheSize();

    default Object putIfAbsent(Object key, Object value) {
        Object existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
        }
        return existingValue;
    }

    default void evictOrClear(Object key) {
        if (Objects.isNull(key)) {
            clear();
            return;
        }
        evict(key);
    }

    default boolean evictIfPresent(Object key) {
        if (Objects.nonNull(key)) {
            evict(key);
            return true;
        }
        return false;
    }

    default boolean invalidate() {
        clear();
        return true;
    }

}
