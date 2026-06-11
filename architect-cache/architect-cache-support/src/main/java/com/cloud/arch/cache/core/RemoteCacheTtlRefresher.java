package com.cloud.arch.cache.core;

import com.cloud.arch.cache.utils.CacheThreadPoolExecutor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class RemoteCacheTtlRefresher {
    // 缓存刷新时间阈值，默认时间-30秒
    public static final Long                DEFAULT_REFRESH_INTERVAL = 30000L;
    // refreshTimeCache 最大容量，防止突发流量下内存暴涨
    public static final long                DEFAULT_MAX_SIZE         = 10000L;
    // Caffeine Cache 自动淘汰过期 key，防止内存无限增长
    private final       Cache<Object, Long> refreshTimeCache;
    // 刷新缓存时间间隔
    private final       Long                refreshInterval;

    public RemoteCacheTtlRefresher() {
        this.refreshInterval = DEFAULT_REFRESH_INTERVAL;
        this.refreshTimeCache = buildCache();
    }

    public RemoteCacheTtlRefresher(long refreshInterval) {
        this.refreshInterval = Math.max(DEFAULT_REFRESH_INTERVAL, refreshInterval);
        this.refreshTimeCache = buildCache();
    }

    private Cache<Object, Long> buildCache() {
        return Caffeine.newBuilder()
                       .maximumSize(DEFAULT_MAX_SIZE)
                       .expireAfterAccess(this.refreshInterval, TimeUnit.MILLISECONDS)
                       .build();
    }

    /**
     * 异步执行缓存刷新
     *
     * @param cacheName   缓存名称
     * @param key         缓存key
     * @param value       缓存值
     * @param refreshTask 刷新任务
     */
    public void refreshTtl(String cacheName, Object key, Object value, CacheTtlRefreshTask refreshTask) {
        long   current   = System.currentTimeMillis();
        String uniqueKey = cacheName + "#" + key.toString();
        refreshTimeCache.asMap().compute(uniqueKey, (k, v) -> {
            if (v == null) {
                return current;
            }
            if (current - v < this.refreshInterval) {
                return v;
            }
            CacheThreadPoolExecutor.run(() -> refreshTask.refreshTtl(key, value));
            return current;
        });
    }

}
