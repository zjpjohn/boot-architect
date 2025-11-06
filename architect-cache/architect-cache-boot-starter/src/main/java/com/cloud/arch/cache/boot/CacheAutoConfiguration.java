package com.cloud.arch.cache.boot;

import com.cloud.arch.cache.boot.extension.HotKeyCacheAutoConfiguration;
import com.cloud.arch.cache.boot.extension.SecondCacheAutoConfiguration;
import com.cloud.arch.cache.config.CloudCacheProperties;
import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.core.RemoteCacheTtlRefresher;
import com.cloud.arch.cache.interceptor.AnnotationCacheAspect;
import com.cloud.arch.cache.interceptor.context.CacheContextContainerFactory;
import com.cloud.arch.cache.metrics.CacheStatsManager;
import com.cloud.arch.cache.metrics.StatsManager;
import com.cloud.arch.cache.support.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@EnableConfigurationProperties(CloudCacheProperties.class)
@AutoConfigureAfter({SecondCacheAutoConfiguration.class, HotKeyCacheAutoConfiguration.class})
public class CacheAutoConfiguration {

    public static final String LAYER_CACHE_MANAGER = "layer_cache_manager";

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.cache", name = "enable-metric", havingValue = "true")
    public StatsManager statsManager(MeterRegistry meterRegistry) {
        return new CacheStatsManager(meterRegistry);
    }

    /**
     * create default key generator
     */
    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    /**
     * create default cache error handler
     */
    @Bean
    @ConditionalOnMissingBean(CacheErrorHandler.class)
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }

    /**
     * cache operation cached source bean
     *
     * @param properties cache properties config
     */
    @Bean
    public CacheOperationCachedSource operationSource(CloudCacheProperties properties, CacheResolver cacheResolver) {
        return new CacheOperationCachedSource(properties, cacheResolver);
    }

    /**
     * cache operation method processor
     */
    @Bean
    public static CacheOperationMethodProcessor cacheOperationMethodProcessor() {
        return new CacheOperationMethodProcessor();
    }

    /**
     * create cache operation context factory
     *
     * @param keyGenerator  key generator bean
     * @param cacheResolver cache resolver bean
     * @param cacheManager  cache manager bean
     */
    @Bean
    public CacheContextContainerFactory operationContextsFactory(KeyGenerator keyGenerator,
                                                                 CacheResolver cacheResolver,
                                                                 CacheManager cacheManager,
                                                                 CacheOperationSource operationSource) {
        return new CacheContextContainerFactory(keyGenerator, cacheResolver, cacheManager, operationSource);
    }

    /**
     * 淘汰缓存事件发布器,提供给业务系统使用，可以手动淘汰缓存
     */
    @Bean
    public CacheEvictPublisher cacheEvictPublisher() {
        return new CacheEvictPublisher();
    }

    /**
     * 缓存淘汰管理器
     * 缓存延迟双删
     */
    @Bean
    public CacheEvictManager cacheEvictManager(CacheManager cacheManager, CloudCacheProperties properties) {
        return new CacheEvictManager(cacheManager, properties);
    }

    /**
     * 二级缓存TTL刷新器
     */
    @Bean
    public RemoteCacheTtlRefresher ttlRefresher(CloudCacheProperties properties) {
        return new RemoteCacheTtlRefresher(properties.getTtlRefreshInterval());
    }


    /**
     * annotation cache aspect bean
     *
     * @param errorHandler             error handler
     * @param operationContextsFactory operation context factory
     */
    @Bean
    public AnnotationCacheAspect annotationCacheAspect(CacheEvictManager cacheEvictManager,
                                                       CacheErrorHandler errorHandler,
                                                       CacheContextContainerFactory operationContextsFactory) {
        return new AnnotationCacheAspect(cacheEvictManager, errorHandler, operationContextsFactory);
    }

}
