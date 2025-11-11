package com.cloud.arch.cache.boot.extension;

import com.cloud.arch.cache.boot.CacheAutoConfiguration;
import com.cloud.arch.cache.config.CachingConfigurer;
import com.cloud.arch.cache.config.CloudCacheProperties;
import com.cloud.arch.cache.config.DefaultCachingConfigurer;
import com.cloud.arch.cache.core.*;
import com.cloud.arch.cache.metrics.StatsManager;
import com.cloud.arch.cache.support.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnMissingClass("com.cloud.arch.cache.extension.HotKeyCacheManager")
@EnableConfigurationProperties(CloudCacheProperties.class)
public class SecondCacheAutoConfiguration {

    /**
     * 默认使用redisson包自动配置的redissonTemplate,
     */
    @Bean
    @ConditionalOnMissingBean(CacheRedisSupplier.class)
    public CacheRedisSupplier redisSupplier(RedissonClient redissonClient) {
        return new DefaultCacheRedisSupplier(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(CacheNodePolicy.class)
    @ConditionalOnProperty(prefix = "com.cloud.cache", name = "enable-local", havingValue = "true")
    public CacheNodePolicy cacheNodePolicy() {
        return new RandomNodePolicy();
    }

    @Bean
    @ConditionalOnMissingBean(RefreshPolicy.class)
    @ConditionalOnProperty(prefix = "com.cloud.cache", name = "enable-local", havingValue = "true")
    public RefreshPolicy refreshPolicy(CloudCacheProperties properties,
                                       CacheNodePolicy cacheNodePolicy,
                                       CacheRedisSupplier redisLoader) {
        return new RedisTopicRefreshPolicy(properties.getRefreshTopic(), redisLoader.get(), cacheNodePolicy);
    }


    @Bean
    public DefaultCachingConfigurer cachingConfigurer(CacheRedisSupplier redisLoader,
                                                      CloudCacheProperties properties,
                                                      RemoteCacheTtlRefresher ttlRefresher,
                                                      ObjectProvider<RefreshPolicy> refreshPolicy,
                                                      ObjectProvider<StatsManager> statsManagers) {
        RefreshPolicy policy       = refreshPolicy.stream().findFirst().orElse(null);
        StatsManager  statsManager = statsManagers.stream().findFirst().orElse(null);
        return new DefaultCachingConfigurer(redisLoader.get(), properties, ttlRefresher, policy, statsManager);
    }

    @Bean
    @ConditionalOnMissingBean({CacheEventListener.class})
    @ConditionalOnProperty(prefix = "com.cloud.cache", name = "enable-local", havingValue = "true")
    public CacheEventListener eventListener(CloudCacheProperties properties,
                                            CacheNodePolicy cacheNodePolicy,
                                            DefaultCachingConfigurer cachingConfigurer) {
        return new RedisRefreshEventListener(properties.getRefreshTopic(),
                                             cachingConfigurer.getCacheManager(),
                                             cacheNodePolicy);
    }


    /**
     * create default cache manager bean
     *
     * @param cachingConfigurer default caching configurer
     */
    @Bean(name = CacheAutoConfiguration.LAYER_CACHE_MANAGER)
    public CacheManager cacheManager(CachingConfigurer cachingConfigurer) {
        return cachingConfigurer.cacheManager();
    }

    /**
     * create default cache resolver bean
     *
     * @param cachingConfigurer default caching configurer
     */
    @Bean
    public CacheResolver cacheResolver(CachingConfigurer cachingConfigurer) {
        return cachingConfigurer.cacheResolver();
    }

}
