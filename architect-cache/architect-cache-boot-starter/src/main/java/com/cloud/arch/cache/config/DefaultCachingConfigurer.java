package com.cloud.arch.cache.config;

import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.core.RefreshPolicy;
import com.cloud.arch.cache.core.RemoteCacheTtlRefresher;
import com.cloud.arch.cache.metrics.StatsManager;
import com.cloud.arch.cache.support.CacheResolver;
import com.cloud.arch.cache.support.RedisCacheManager;
import com.cloud.arch.cache.support.SimpleCacheResolver;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
public class DefaultCachingConfigurer implements CachingConfigurer {

    private final RedissonClient          redissonClient;
    private final CloudCacheProperties    cloudCacheProperties;
    private final RedisCacheManager       cacheManager;
    private final RefreshPolicy           refreshPolicy;
    private final RemoteCacheTtlRefresher ttlRefresher;
    private final StatsManager            statsManager;

    public DefaultCachingConfigurer(RedissonClient redissonClient,
                                    CloudCacheProperties cloudCacheProperties,
                                    RemoteCacheTtlRefresher ttlRefresher,
                                    RefreshPolicy refreshPolicy,
                                    StatsManager statsManager) {
        this.redissonClient       = redissonClient;
        this.cloudCacheProperties = cloudCacheProperties;
        this.refreshPolicy        = refreshPolicy;
        this.ttlRefresher         = ttlRefresher;
        this.statsManager         = statsManager;
        this.cacheManager         = this.build();
    }

    private RedisCacheManager build() {
        //开启本地缓存
        if (cloudCacheProperties.isEnableLocal()) {
            Assert.state(StringUtils.hasText(cloudCacheProperties.getRefreshTopic()), "multi level cache refresh topic must not be null.");
            ScheduledThreadPoolExecutor scheduleExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime()
                                                                                                  .availableProcessors()
                                                                                                   / 2, new DefaultThreadFactory("thales-cache-scheduler"));
            return new RedisCacheManager(statsManager, redissonClient, refreshPolicy, ttlRefresher, scheduleExecutor);
        }
        //未开启本地缓存
        return new RedisCacheManager(statsManager, redissonClient, ttlRefresher);
    }

    public RedisCacheManager getCacheManager() {
        return cacheManager;
    }

    public RefreshPolicy getRefreshPolicy() {
        return refreshPolicy;
    }

    /**
     * return cache manager
     * custom cache manager can be used
     */
    @Override
    public CacheManager cacheManager() {
        return this.cacheManager;
    }

    /**
     * create or build cache resolver
     */
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager);
    }

}
