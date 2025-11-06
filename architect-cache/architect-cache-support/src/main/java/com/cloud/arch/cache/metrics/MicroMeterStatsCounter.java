package com.cloud.arch.cache.metrics;

import com.cloud.arch.cache.core.Cache;
import io.micrometer.core.instrument.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MicroMeterStatsCounter implements StatsCounter {

    private static final String GET_COUNTER_NAME = "cache.gets";
    private static final String LOAD_TIMER_NAME  = "cache.loads";
    private static final String CACHE_EVICT_NAME = "cache.evictions";
    private static final String CACHE_SIZE_NAME  = "cache.size";
    private static final String TAG_RESULT_NAME  = "result";

    private final MeterRegistry       registry;
    private final Tags                tags;
    private final Counter             hitL1Count;
    private final Counter             hitCount;
    private final Counter             missCount;
    private final Timer               loadSuccess;
    private final Timer               loadFail;
    private final DistributionSummary evictCount;

    public MicroMeterStatsCounter(MeterRegistry registry, String cacheName) {
        this.registry    = registry;
        this.tags        = Tags.of("cache", cacheName);
        this.hitL1Count  = Counter.builder(GET_COUNTER_NAME).tags(TAG_RESULT_NAME, "hitLocal").tags(tags)
                                  .description("The number of times local cache lookup methods have returned a cached value.")
                                  .register(registry);
        this.hitCount    = Counter.builder(GET_COUNTER_NAME).tag(TAG_RESULT_NAME, "hit").tags(tags)
                                  .description("The number of all times cache lookup methods have returned a cached value.")
                                  .register(registry);
        this.missCount   = Counter.builder(GET_COUNTER_NAME).tag(TAG_RESULT_NAME, "miss").tags(tags)
                                  .description("The number of times cache lookup methods have returned an uncached (newly load) value.")
                                  .register(registry);
        this.loadSuccess = Timer.builder(LOAD_TIMER_NAME).tag(TAG_RESULT_NAME, "success").tags(tags)
                                .description("Successful cache loads of cache.").register(registry);
        this.loadFail    = Timer.builder(LOAD_TIMER_NAME).tag(TAG_RESULT_NAME, "failure").tags(tags)
                                .description("Failed cache loads of local cache.").register(registry);
        this.evictCount  = DistributionSummary.builder(CACHE_EVICT_NAME).tag(TAG_RESULT_NAME, "evict").tags(tags)
                                              .description("Entries evicted from local cache.").register(registry);
    }

    public void registerSizeMetric(Cache cache) {
        Gauge.builder(CACHE_SIZE_NAME, cache, Cache::cacheSize).tags(tags)
             .description("The approximate number of entries in cache.").register(registry);
    }

    /**
     * 缓存命中统计
     *
     * @param localCache 是否是本地缓存命中
     */
    @Override
    public void recordHits(int count, boolean localCache) {
        if (localCache) {
            this.hitL1Count.increment(count);
        }
        this.hitCount.increment(count);
    }

    /**
     * 缓存未命中统计
     */
    @Override
    public void recordMisses(int count) {
        this.missCount.increment(count);
    }

    /**
     * 加载缓存成功统计
     */
    @Override
    public void recordLoadSuccess(long loadTime) {
        this.loadSuccess.record(loadTime, TimeUnit.NANOSECONDS);
    }

    /**
     * 加载缓存失败统计
     */
    @Override
    public void recordLoadFail(long loadTime) {
        this.loadFail.record(loadTime, TimeUnit.NANOSECONDS);
    }

    /**
     * 缓存失效统计
     */
    @Override
    public void recordEvict(long count) {
        this.evictCount.record(count);
    }

    /**
     * 缓存失效统计
     */
    @Override
    public void recordEvict(Supplier<Long> supplier) {
        Optional.ofNullable(supplier).map(Supplier::get).filter(count -> count > 0).ifPresent(this.evictCount::record);
    }

    /**
     * 复制当前统计器的统计值
     */
    @Override
    public CacheStats snapshot() {
        return new CacheStats((long) this.hitCount.count(), (long) this.missCount.count(), (long) this.hitL1Count.count(), this.loadSuccess.count(), this.loadFail.count(), this.totalLoadTime(), this.evictCount.count(), (long) this.evictCount.totalAmount());
    }

    private long totalLoadTime() {
        return (long) this.loadSuccess.totalTime(TimeUnit.NANOSECONDS)
               + (long) this.loadFail.totalTime(TimeUnit.NANOSECONDS);
    }

}
