package com.cloud.arch.hotkey.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.Maps;

import java.util.Map;

public class CacheManager {

    /**
     * 热key时间窗缓存
     */
    private final Map<String, Cache<String, Object>> CACHE_MAP = Maps.newConcurrentMap();

    public Cache<String, Object> getCache(String appName, String cacheName) {
        String cacheKey = this.cacheKey(appName, cacheName);
        if (CACHE_MAP.get(cacheKey) == null) {
            Cache<String, Object> cache = CacheBuilder.buildRecentHotKeyCache();
            CACHE_MAP.put(cacheKey, cache);
        }
        return CACHE_MAP.get(cacheKey);
    }

    public void clearCache(String appName, String cacheName) {
        String                cacheKey = this.cacheKey(appName, cacheName);
        Cache<String, Object> cache    = CACHE_MAP.get(cacheKey);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    public Map<String, Integer> cacheSize() {
        Map<String, Integer> map = Maps.newHashMap();
        for (String key : CACHE_MAP.keySet()) {
            Cache<String, Object> cache = CACHE_MAP.get(key);
            int                   size  = cache.asMap().size();
            map.put(key, size);
        }
        return map;
    }

    private String cacheKey(String appName, String cacheName) {
        return appName + "/" + cacheName;
    }
}
