package com.cloud.arch.cache.boot.extension;

import com.cloud.arch.cache.boot.CacheAutoConfiguration;
import com.cloud.arch.cache.config.CloudCacheProperties;
import com.cloud.arch.cache.core.CacheEventListener;
import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshPolicy;
import com.cloud.arch.cache.core.RemoteCacheTtlRefresher;
import com.cloud.arch.cache.extension.HotKeyCacheManager;
import com.cloud.arch.cache.extension.HotKeyRefreshEventListener;
import com.cloud.arch.cache.extension.HotKeyWatcherFactoryBean;
import com.cloud.arch.cache.extension.ScheduledPusherFactoryBean;
import com.cloud.arch.cache.metrics.StatsManager;
import com.cloud.arch.cache.props.HotKeyCacheProperties;
import com.cloud.arch.cache.support.*;
import com.cloud.arch.hotkey.config.EtcdConfigCenter;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.core.key.*;
import com.cloud.arch.hotkey.core.rule.KeyRuleManager;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.network.worker.HotKeyWorkerManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass(name = "com.cloud.arch.cache.extension.HotKeyCacheManager")
@ConditionalOnProperty(prefix = "com.cloud.cache.hotkey", name = "etcd-server")
@EnableConfigurationProperties(CloudCacheProperties.class)
public class HotKeyCacheAutoConfiguration {

    /**
     * 默认使用redisson包自动配置的redissonTemplate,
     */
    @Bean
    @ConditionalOnMissingBean(CacheRedisSupplier.class)
    public CacheRedisSupplier redisSupplier(RedissonClient redissonClient) {
        return new DefaultCacheRedisSupplier(redissonClient);
    }

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.cache.hotkey")
    public HotKeyCacheProperties hotKeyCacheProperties() {
        return new HotKeyCacheProperties();
    }

    /**
     * 热key规则管理
     */
    @Bean
    public KeyRuleManager keyRuleManager() {
        return new KeyRuleManager();
    }

    /**
     * 热key数据收集器
     */
    @Bean
    public IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector() {
        return new TurnKeyCollector();
    }

    /**
     * 热key规则统计数据收集器
     */
    @Bean
    public IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector() {
        return new TurnCountCollector();
    }

    /**
     * ETCD配置管理客户端
     */
    @Bean
    public IConfigCenter configCenter(HotKeyCacheProperties properties) {
        return new EtcdConfigCenter(properties.getEtcdServer());
    }

    /**
     * 热key探测worker集群管理
     */
    @Bean
    public HotKeyWorkerManager workerManager(HotKeyCacheProperties properties, IConfigCenter configCenter) {
        return new HotKeyWorkerManager(properties.getAppName(), configCenter);
    }

    /**
     * 带热key探测缓存
     */
    @Bean
    public HotKeyCache hotKeyCache(HotKeyCacheProperties properties,
                                   KeyRuleManager keyRuleManager,
                                   IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                   IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        return new HotKeyCache(properties.getAppName(), keyRuleManager, hotKeyCollector, keyCountCollector);
    }

    /**
     * 集群缓存数据刷新策略器
     */
    @Bean
    @ConditionalOnMissingBean(RefreshPolicy.class)
    public RefreshPolicy refreshPolicy(CloudCacheProperties properties,
                                       CacheNodePolicy cacheNodePolicy,
                                       CacheRedisSupplier redisLoader) {
        return new RedisTopicRefreshPolicy(properties.getRefreshTopic(), redisLoader.get(), cacheNodePolicy);
    }

    /**
     * 热key探测的缓存管理器
     */
    @Bean(name = CacheAutoConfiguration.LAYER_CACHE_MANAGER)
    public HotKeyCacheManager cacheManager(CacheRedisSupplier redisLoader,
                                           HotKeyCache hotKeyCache,
                                           RemoteCacheTtlRefresher ttlRefresher,
                                           RefreshPolicy refreshPolicy,
                                           ObjectProvider<StatsManager> statsManagers) {
        StatsManager statsManager = statsManagers.stream().findFirst().orElse(null);
        return new HotKeyCacheManager(redisLoader.get(), hotKeyCache, ttlRefresher, refreshPolicy, statsManager);
    }

    /**
     * 热key探测cache refresh
     */
    @Bean
    public CacheResolver cacheResolver(HotKeyCacheManager cacheManager) {
        return new SimpleCacheResolver(cacheManager);
    }

    /**
     * 缓存节点标识生成策略
     */
    @Bean
    @ConditionalOnMissingBean(CacheNodePolicy.class)
    public CacheNodePolicy cacheNodePolicy() {
        return new RandomNodePolicy();
    }

    /**
     * 缓存事件处理监听器
     */
    @Bean
    public CacheEventListener eventListener(CloudCacheProperties properties,
                                            HotKeyCacheManager hotKeyCacheManager,
                                            CacheNodePolicy cacheNodePolicy) {
        return new HotKeyRefreshEventListener(properties.getRefreshTopic(), hotKeyCacheManager, cacheNodePolicy);
    }

    /**
     * 热key创建事件订阅处理器
     */
    @Bean
    public ReceiveNewKeySubscriber receiveNewKeySubscriber(HotKeyCache hotKeyCache) {
        return new ReceiveNewKeySubscriber(hotKeyCache);
    }

    /**
     * 热key以及规则配置监听器
     */
    @Bean
    public HotKeyWatcherFactoryBean hotKeyDetectWatcher(HotKeyCacheProperties properties,
                                                        IConfigCenter configCenter,
                                                        KeyRuleManager keyRuleManager) {
        return new HotKeyWatcherFactoryBean(properties.getAppName(), configCenter, keyRuleManager);
    }

    /**
     * 热key统计定时上报
     */
    @Bean
    public ScheduledPusherFactoryBean workerScheduledPusher(HotKeyCacheProperties properties,
                                                            HotKeyWorkerManager workerManager,
                                                            IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                                            IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        return new ScheduledPusherFactoryBean(properties, workerManager, hotKeyCollector, keyCountCollector);
    }

}
