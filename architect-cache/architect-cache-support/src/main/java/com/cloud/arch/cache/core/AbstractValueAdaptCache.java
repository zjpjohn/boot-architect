package com.cloud.arch.cache.core;


import com.cloud.arch.cache.metrics.StatsCounter;

import java.util.concurrent.Callable;

@SuppressWarnings("unchecked")
public abstract class AbstractValueAdaptCache implements Cache {

    private final String  name;
    private final boolean allowNullValue;

    protected AbstractValueAdaptCache(String name, boolean allowNullValue) {
        this.name           = name;
        this.allowNullValue = allowNullValue;
    }

    /**
     * 当前缓存是否支持缓存null值
     */
    public boolean isAllowNullValue() {
        return allowNullValue;
    }

    /**
     * 缓存对象的名称.
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 缓存的实际实现对象
     */
    @Override
    public Cache getCache() {
        return this;
    }

    /**
     * set cache stats time ticker
     *
     * @param ticker time ticker
     */
    public void statsTicker(Ticker ticker) {
    }

    /**
     * time ticker used by stats counter
     */
    public Ticker statsTicker() {
        return Ticker.disableTicker();
    }

    /**
     * set cache stats counter
     *
     * @param statsCounter cache stats counter
     */
    public void statsCounter(StatsCounter statsCounter) {
    }

    /**
     * stats counter
     */
    public StatsCounter statsCounter() {
        return StatsCounter.disabledStatsCounter();
    }

    /**
     * stats counter wrapped value loader
     *
     * @param valueLoader cache value loader
     */
    public <T> T statsWrappedLoad(Callable<T> valueLoader) throws Exception {
        if (statsTicker() == Ticker.disableTicker()) {
            return valueLoader.call();
        }
        statsCounter().recordMisses(1);
        long startTime = statsTicker().read();
        T    value;
        try {
            value = valueLoader.call();
        } catch (Exception error) {
            statsCounter().recordLoadFail(statsTicker().read() - startTime);
            throw error;
        }
        long loadTime = statsTicker().read() - startTime;
        if (value == null) {
            statsCounter().recordLoadFail(loadTime);
        } else {
            statsCounter().recordLoadSuccess(loadTime);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    /**
     * 将缓存存储值转换成实际值 主要是对null值处理
     *
     * @param storeValue 缓存存储值
     */
    protected Object fromStoreValue(Object storeValue) {
        if (this.allowNullValue && storeValue instanceof NullValue) {
            return null;
        }
        return storeValue;
    }

    /**
     * 将缓存值转换成存储的值 主要是对null值处理
     *
     * @param userValue 缓存存储值
     */
    protected Object toStoreValue(Object userValue) {
        if (this.allowNullValue && userValue == null) {
            return NullValue.INSTANCE;
        }
        return userValue;
    }

    /**
     * 缓存存储数据转换为实际数据
     *
     * @param storeValue 缓存存储值
     */
    protected Object toValue(Object storeValue) {
        return storeValue != null ? fromStoreValue(storeValue) : null;
    }

}
