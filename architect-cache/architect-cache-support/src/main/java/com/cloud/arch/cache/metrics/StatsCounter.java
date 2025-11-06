package com.cloud.arch.cache.metrics;

import java.util.function.Supplier;

public interface StatsCounter {

    static StatsCounter disabledStatsCounter() {
        return DisabledStatsCounter.INSTANCE;
    }

    /**
     * 缓存命中统计
     */
    void recordHits(int count,boolean local);

    /**
     * 缓存未命中统计
     */
    void recordMisses(int count);

    /**
     * 加载缓存成功统计
     */
    void recordLoadSuccess(long loadTime);

    /**
     * 加载缓存失败统计
     */
    void recordLoadFail(long loadTime);

    /**
     * 缓存失效统计
     */
    void recordEvict(long count);

    /**
     * 缓存失效统计
     */
    void recordEvict(Supplier<Long> supplier);

    /**
     * 复制当前统计器的统计值
     */
    CacheStats snapshot();

}
