package com.cloud.arch.cache.metrics;

public final class CacheStats {

    private static final CacheStats EMPTY_STATS = new CacheStats();

    // 本地缓存命中率
    private long hitL1Count = 0;
    // 命中缓存
    private long hitCount = 0;
    // 未命中缓存
    private long missCount = 0;
    // 缓存加载成功
    private long loadSuccessCount = 0;
    // 缓存加载失败
    private long loadFailCount = 0;
    // 缓存加载总时间
    private long totalLoadTime = 0;
    // 缓存失效次数
    private long evictCount = 0;
    // 缓存失效元素总次数
    private long evictWeight = 0;

    public CacheStats() {

    }

    public CacheStats(long hitCount, long missCount, long hitL1Count, long loadSuccessCount, long loadFailCount,
        long totalLoadTime, long evictCount, long evictWeight) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.hitL1Count = hitL1Count;
        this.loadSuccessCount = loadSuccessCount;
        this.loadFailCount = loadFailCount;
        this.totalLoadTime = totalLoadTime;
        this.evictCount = evictCount;
        this.evictWeight = evictWeight;
    }

    public static CacheStats empty() {
        return EMPTY_STATS;
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public long getHitL1Count() {
        return hitL1Count;
    }

    public long getLoadSuccessCount() {
        return loadSuccessCount;
    }

    public long getLoadFailCount() {
        return loadFailCount;
    }

    public long getTotalLoadTime() {
        return totalLoadTime;
    }

    public long getEvictCount() {
        return evictCount;
    }

    public long getEvictWeight() {
        return evictWeight;
    }

    public long requestCount() {
        return saturatedAdd(hitCount, missCount);
    }

    public double hitL1Rate() {
        return hitCount == 0 ? 1.0 : (double)hitL1Count / hitCount;
    }

    public double hitRate() {
        long requestCount = requestCount();
        return requestCount == 0 ? 1.0 : (double)hitCount / requestCount;
    }

    public double missRate() {
        long requestCount = requestCount();
        return requestCount == 0 ? 1.0 : (double)missCount / requestCount;
    }

    public double averageLoadPenalty() {
        long totalLoadCount = saturatedAdd(loadSuccessCount, loadFailCount);
        return (totalLoadCount == 0) ? 0.0 : (double)totalLoadTime / totalLoadCount;
    }

    public double loadFailRate() {
        long totalLoadCount = saturatedAdd(loadSuccessCount, loadFailCount);
        return totalLoadCount == 0 ? 1.0 : (double)loadFailCount / totalLoadCount;
    }

    private static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 || (a ^ naiveSum) >= 0) {
            return naiveSum;
        }
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    private static long saturatedMinus(long a, long b) {
        long naiveDifference = a - b;
        if ((a ^ b) >= 0 || (a ^ naiveDifference) >= 0) {
            return naiveDifference;
        }
        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }

}
