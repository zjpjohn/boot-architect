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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * 默认缓存配置器，负责构建 {@link RedisCacheManager} 及其依赖的调度线程池、刷新策略、统计管理器等，
 * 根据 {@link CloudCacheProperties#isEnableLocal()} 决定是否启用 L1 本地缓存
 */
@Slf4j
public class DefaultCachingConfigurer implements CachingConfigurer, DisposableBean {

    private final RedissonClient              redissonClient;
    private final CloudCacheProperties        cloudCacheProperties;
    private final RedisCacheManager           cacheManager;
    private final RefreshPolicy               refreshPolicy;
    private final RemoteCacheTtlRefresher     ttlRefresher;
    private final StatsManager                statsManager;
    private       ScheduledThreadPoolExecutor scheduleExecutor;

    public DefaultCachingConfigurer(RedissonClient redissonClient, CloudCacheProperties cloudCacheProperties, RemoteCacheTtlRefresher ttlRefresher, RefreshPolicy refreshPolicy, StatsManager statsManager) {
        this.redissonClient = redissonClient;
        this.cloudCacheProperties = cloudCacheProperties;
        this.refreshPolicy = refreshPolicy;
        this.ttlRefresher = ttlRefresher;
        this.statsManager = statsManager;
        this.cacheManager = this.build();
    }

    private RedisCacheManager build() {
        //开启本地缓存
        if (cloudCacheProperties.isEnableLocal()) {
            Assert.state(StringUtils.hasText(cloudCacheProperties.getRefreshTopic()), "multi level cache refresh topic must not be null.");
            int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            this.scheduleExecutor = new ScheduledThreadPoolExecutor(poolSize, new DefaultThreadFactory("cache-scheduler"));
            this.scheduleExecutor.setRemoveOnCancelPolicy(true);
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

    public RedissonClient getRedissonClient() {
        return redissonClient;
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

    @Override
    public void destroy() throws Exception {
        if (scheduleExecutor != null) {
            scheduleExecutor.shutdown();
        }
    }

}
