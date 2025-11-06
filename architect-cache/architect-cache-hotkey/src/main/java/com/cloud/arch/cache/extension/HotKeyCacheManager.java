package com.cloud.arch.cache.extension;


import com.cloud.arch.cache.core.*;
import com.cloud.arch.cache.metrics.StatsCounter;
import com.cloud.arch.cache.metrics.StatsManager;
import com.cloud.arch.hotkey.core.key.HotKeyCache;
import org.redisson.api.RedissonClient;

public class HotKeyCacheManager extends AbstractCacheManager {

    private final RedissonClient          redissonClient;
    private final HotKeyCache             hotKeyCache;
    private final RefreshPolicy           refreshPolicy;
    private final RemoteCacheTtlRefresher ttlRefresher;
    private final StatsManager            statsManager;

    public HotKeyCacheManager(RedissonClient redissonClient,
                              HotKeyCache hotKeyCache,
                              RemoteCacheTtlRefresher ttlRefresher,
                              RefreshPolicy refreshPolicy,
                              StatsManager statsManager) {
        this.redissonClient = redissonClient;
        this.hotKeyCache    = hotKeyCache;
        this.refreshPolicy  = refreshPolicy;
        this.ttlRefresher   = ttlRefresher;
        this.statsManager   = statsManager;
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    public HotKeyCache getHotKeyCache() {
        return hotKeyCache;
    }

    /**
     * 缓存监控指标管理器
     */
    @Override
    public StatsManager statsManager() {
        if (this.statsManager != null) {
            return this.statsManager;
        }
        return StatsManager.disabledManager();
    }

    /**
     * 创建redis带自动热点数据探测的缓存实例
     *
     * @param name     缓存名称
     * @param settings 缓存配置信息
     */
    @Override
    public Cache getMissingCache(String name, CacheSettings settings) {
        HotKeyRedisCache hotKeyRedisCache = new HotKeyRedisCache(name, settings, this.refreshPolicy, redissonClient, ttlRefresher, hotKeyCache);
        StatsCounter     statsCounter     = statsManager().statsCounter(hotKeyRedisCache);
        hotKeyRedisCache.statsTicker(statsManager().timeTicker());
        hotKeyRedisCache.statsCounter(statsCounter);
        return hotKeyRedisCache;
    }

}
