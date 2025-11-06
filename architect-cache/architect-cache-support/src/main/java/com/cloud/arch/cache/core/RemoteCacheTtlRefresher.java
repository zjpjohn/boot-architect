package com.cloud.arch.cache.core;

import com.cloud.arch.cache.utils.CacheThreadPoolExecutor;
import com.google.common.collect.Maps;

import java.util.concurrent.ConcurrentMap;

public class RemoteCacheTtlRefresher {
    // 缓存刷新时间阈值，默认时间-30秒
    public static final Long                        DEFAULT_REFRESH_INTERVAL = 30000L;
    // 最新缓存数据刷新时间
    private final       ConcurrentMap<Object, Long> refreshTimeCache         = Maps.newConcurrentMap();
    // 刷新缓存时间间隔
    private final       Long                        refreshInterval;

    public RemoteCacheTtlRefresher() {
        this.refreshInterval = DEFAULT_REFRESH_INTERVAL;
    }

    public RemoteCacheTtlRefresher(long refreshInterval) {
        this.refreshInterval = Math.max(DEFAULT_REFRESH_INTERVAL, refreshInterval);
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
        long   current     = System.currentTimeMillis();
        String uniqueKey   = cacheName + key.toString();
        Long   lastRefresh = refreshTimeCache.computeIfAbsent(uniqueKey, k -> current);
        if (current - lastRefresh < this.refreshInterval) {
            return;
        }
        refreshTimeCache.put(uniqueKey, current);
        CacheThreadPoolExecutor.run(() -> refreshTask.refreshTtl(key, value));
    }

}
