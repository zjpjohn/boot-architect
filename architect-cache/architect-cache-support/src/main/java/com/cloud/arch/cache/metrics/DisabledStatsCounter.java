package com.cloud.arch.cache.metrics;


import java.util.function.Supplier;

public enum DisabledStatsCounter implements StatsCounter {

    INSTANCE;


    /**
     * 缓存命中统计
     */
    @Override
    public void recordHits(int count, boolean local) {

    }

    /**
     * 缓存未命中统计
     */
    @Override
    public void recordMisses(int count) {

    }

    /**
     * 加载缓存成功统计
     */
    @Override
    public void recordLoadSuccess(long loadTime) {

    }

    /**
     * 加载缓存失败统计
     */
    @Override
    public void recordLoadFail(long loadTime) {

    }

    /**
     * 缓存失效统计
     */
    @Override
    public void recordEvict(long count) {

    }

    /**
     * 缓存失效统计
     */
    @Override
    public void recordEvict(Supplier<Long> supplier) {

    }


    /**
     * 复制当前统计器的统计值
     */
    @Override
    public CacheStats snapshot() {
        return CacheStats.empty();
    }

}
