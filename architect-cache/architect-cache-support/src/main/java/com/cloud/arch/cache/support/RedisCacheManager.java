package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.*;
import com.cloud.arch.cache.metrics.StatsCounter;
import com.cloud.arch.cache.metrics.StatsManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class RedisCacheManager extends AbstractCacheManager {

    private final RedissonClient           redissonClient;
    private final boolean                  enableLocal;
    private final RemoteCacheTtlRefresher  ttlRefresher;
    private final StatsManager             statsManager;
    private       RefreshPolicy            refreshPolicy;
    private       ScheduledExecutorService scheduledExecutor;

    public RedisCacheManager(StatsManager statsManager,
                             RedissonClient redissonClient,
                             RemoteCacheTtlRefresher ttlRefresher) {
        this.enableLocal    = false;
        this.statsManager   = statsManager;
        this.ttlRefresher   = ttlRefresher;
        this.redissonClient = redissonClient;
    }

    public RedisCacheManager(StatsManager statsManager,
                             RedissonClient redissonClient,
                             RefreshPolicy refreshPolicy,
                             RemoteCacheTtlRefresher ttlRefresher,
                             ScheduledExecutorService scheduledExecutor) {
        this.enableLocal       = true;
        this.statsManager      = statsManager;
        this.ttlRefresher      = ttlRefresher;
        this.refreshPolicy     = refreshPolicy;
        this.redissonClient    = redissonClient;
        this.scheduledExecutor = scheduledExecutor;
    }

    public boolean isEnableLocal() {
        return enableLocal;
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
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
     * 动态激活L1本地缓存
     * 1.全局配置启用本地缓存
     * 2.自定义配置本地缓存信息否则采用默认本地缓存配置信息
     *
     * @param name L2缓存名称
     */
    @Override
    public void activateLocal(String name) {
        AbstractRemoteCache remoteCache = (AbstractRemoteCache) this.getCache(name);
        if (!enableLocal || remoteCache == null || remoteCache.isActivatedLocal()) {
            return;
        }
        CacheSettings cacheSettings = remoteCache.getSettings();
        CaffeineLocalCache localCache = new CaffeineLocalCache(name,
                                                               cacheSettings.isAllowNullValue(),
                                                               cacheSettings.getLocal(),
                                                               refreshPolicy,
                                                               remoteCache,
                                                               scheduledExecutor);
        remoteCache.activateLocal(localCache);
    }

    /**
     * 动态删除L1缓存
     *
     * @param name 缓存名称
     */
    @Override
    public void detachLocal(String name) {
        AbstractRemoteCache remoteCache = (AbstractRemoteCache) this.getCache(name);
        if (remoteCache != null && remoteCache.isActivatedLocal()) {
            remoteCache.detachLocal();
        }
    }

    /**
     * 创建指定名称和配置的L2缓存
     *
     * @param name     缓存名称
     * @param settings 缓存配置信息
     */
    @Override
    public Cache getMissingCache(String name, CacheSettings settings) {
        // 创建redis二级缓存实例
        log.info("cache name:{},setting:{}", name, settings);
        RedisRemoteCache redisCache   = new RedisRemoteCache(name, settings, redissonClient, ttlRefresher);
        Ticker           statsTicker  = statsManager().timeTicker();
        StatsCounter     statsCounter = statsManager().statsCounter(redisCache);
        redisCache.statsTicker(statsTicker);
        redisCache.statsCounter(statsCounter);

        // 缓存初始化指定启用L1缓存
        if (enableLocal && settings.isEnableLocal() && settings.getLocal() != null) {
            CaffeineLocalCache localCache = new CaffeineLocalCache(name,
                                                                   settings.isAllowNullValue(),
                                                                   settings.getLocal(),
                                                                   refreshPolicy,
                                                                   redisCache,
                                                                   scheduledExecutor);
            redisCache.activateLocal(localCache);
        }
        return redisCache;
    }

}
