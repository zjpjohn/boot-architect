package com.cloud.arch.cache.warmup.config;

import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.warmup.core.WarmUpArgsProvider;
import com.cloud.arch.cache.warmup.core.WarmUpCoordinator;
import com.cloud.arch.cache.warmup.core.WarmUpExecutor;
import com.cloud.arch.cache.warmup.core.WarmUpRegistry;
import com.cloud.arch.cache.warmup.core.WarmUpScanner;
import com.cloud.arch.cache.warmup.core.WarmUpTemplate;
import com.cloud.arch.cache.warmup.endpoint.WarmUpEndpoint;
import com.cloud.arch.cache.warmup.metrics.WarmUpMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 缓存预热自动装配，依赖 CacheManager 存在时生效
 */
@Configuration
@ConditionalOnBean(CacheManager.class)
@EnableConfigurationProperties(WarmUpProperties.class)
public class WarmUpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WarmUpArgsProvider warmUpArgsProvider(WarmUpProperties properties) {
        return new ConfigWarmUpArgsProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(WarmUpMetrics.class)
    public WarmUpMetrics warmUpMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new WarmUpMetrics(meterRegistryProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public WarmUpCoordinator warmUpCoordinator(ObjectProvider<RedissonClient> redissonClientProvider) {
        return new WarmUpCoordinator(redissonClientProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public WarmUpExecutor warmUpExecutor(WarmUpCoordinator coordinator, WarmUpMetrics metrics, WarmUpProperties properties) {
        return new WarmUpExecutor(coordinator, metrics, properties.getLockWaitSeconds(), properties.getTimeoutSeconds());
    }

    @Bean
    public WarmUpRegistry warmUpRegistry() {
        return new WarmUpRegistry();
    }

    @Bean
    public WarmUpScanner warmUpScanner(WarmUpRegistry registry, WarmUpExecutor executor, WarmUpArgsProvider argsProvider, WarmUpProperties properties, WarmUpMetrics metrics) {
        return new WarmUpScanner(registry, executor, argsProvider, properties, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public WarmUpTemplate warmUpTemplate(WarmUpRegistry registry, WarmUpExecutor executor, WarmUpArgsProvider argsProvider) {
        return new WarmUpTemplate(registry, executor, argsProvider);
    }

    @Bean
    @ConditionalOnClass(RequestMapping.class)
    @ConditionalOnProperty(prefix = "com.cloud.cache.warm-up",
            name = "rest-endpoint",
            havingValue = "true",
            matchIfMissing = true)
    public WarmUpEndpoint warmUpEndpoint(WarmUpTemplate template) {
        return new WarmUpEndpoint(template);
    }

}
