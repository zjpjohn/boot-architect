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
     * Redis 客户端供应器，默认使用 Redisson 自动配置的 RedissonClient，
     * 多 Redis 源场景下可通过自定义 {@link CacheRedisSupplier} Bean 覆盖
     */
    @Bean
    @ConditionalOnMissingBean(CacheRedisSupplier.class)
    public CacheRedisSupplier redisSupplier(RedissonClient redissonClient) {
        return new DefaultCacheRedisSupplier(redissonClient);
    }

    /**
     * 热 key 探测配置属性，前缀 com.cloud.cache.hotkey
     */
    @Bean
    @ConfigurationProperties(prefix = "com.cloud.cache.hotkey")
    public HotKeyCacheProperties hotKeyCacheProperties() {
        return new HotKeyCacheProperties();
    }

    /**
     * 热 key 规则管理器，负责维护哪些 key 被标记为热点
     */
    @Bean
    public KeyRuleManager keyRuleManager() {
        return new KeyRuleManager();
    }

    /**
     * 热 key 数据收集器（轮转桶算法），统计每个 key 的访问频率
     */
    @Bean
    public IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector() {
        return new TurnKeyCollector();
    }

    /**
     * 热 key 规则命中统计收集器，统计规则匹配的 key 计数
     */
    @Bean
    public IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector() {
        return new TurnCountCollector();
    }

    /**
     * ETCD 配置中心客户端，用于热 key 规则的分发和同步
     */
    @Bean
    public IConfigCenter configCenter(HotKeyCacheProperties properties) {
        return new EtcdConfigCenter(properties.getEtcdServer());
    }

    /**
     * 热 key 探测 Worker 集群管理器，负责 Worker 注册和心跳
     */
    @Bean
    public HotKeyWorkerManager workerManager(HotKeyCacheProperties properties, IConfigCenter configCenter) {
        return new HotKeyWorkerManager(properties.getAppName(), configCenter);
    }

    /**
     * 热 key 缓存包装器，组合热 key 探测能力和二级缓存读写
     */
    @Bean
    public HotKeyCache hotKeyCache(HotKeyCacheProperties properties,
                                   KeyRuleManager keyRuleManager,
                                   IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                   IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        return new HotKeyCache(properties.getAppName(), keyRuleManager, hotKeyCollector, keyCountCollector);
    }

    /**
     * 集群缓存一致性策略，通过 Redis Pub/Sub 广播缓存变更事件
     */
    @Bean
    @ConditionalOnMissingBean(RefreshPolicy.class)
    public RefreshPolicy refreshPolicy(CloudCacheProperties properties,
                                       CacheNodePolicy cacheNodePolicy,
                                       CacheRedisSupplier redisLoader) {
        return new RedisTopicRefreshPolicy(properties.getRefreshTopic(), redisLoader.get(), cacheNodePolicy);
    }

    /**
     * 热 key 探测模式下的缓存管理器，将热 key 探针注入 RedisCacheManager
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
     * 缓存解析器，从缓存操作元数据中解析出对应的 Cache 实例
     */
    @Bean
    public CacheResolver cacheResolver(HotKeyCacheManager cacheManager) {
        return new SimpleCacheResolver(cacheManager);
    }

    /**
     * 缓存节点标识策略，用于 Pub/Sub 消息去重（区分消息来源节点）
     */
    @Bean
    @ConditionalOnMissingBean(CacheNodePolicy.class)
    public CacheNodePolicy cacheNodePolicy() {
        return new RandomNodePolicy();
    }

    /**
     * 热 key 模式下缓存事件监听器，接收集群失效消息并淘汰本地 L1
     */
    @Bean
    public CacheEventListener eventListener(CloudCacheProperties properties,
                                            HotKeyCacheManager hotKeyCacheManager,
                                            CacheNodePolicy cacheNodePolicy) {
        return new HotKeyRefreshEventListener(properties.getRefreshTopic(), hotKeyCacheManager, cacheNodePolicy);
    }

    /**
     * 新热 key 创建事件订阅处理器，接收 Worker 推送的新热 key 并写入本地规则
     */
    @Bean
    public ReceiveNewKeySubscriber receiveNewKeySubscriber(HotKeyCache hotKeyCache) {
        return new ReceiveNewKeySubscriber(hotKeyCache);
    }

    /**
     * 热 key 及规则配置变更监听器（基于 ETCD watch），检测到变更后刷新本地规则
     */
    @Bean
    public HotKeyWatcherFactoryBean hotKeyDetectWatcher(HotKeyCacheProperties properties,
                                                        IConfigCenter configCenter,
                                                        KeyRuleManager keyRuleManager) {
        return new HotKeyWatcherFactoryBean(properties.getAppName(), configCenter, keyRuleManager);
    }

    /**
     * Worker 定时上报器，定期将本节点收集的热 key 统计数据推送到 Worker 集群
     */
    @Bean
    public ScheduledPusherFactoryBean workerScheduledPusher(HotKeyCacheProperties properties,
                                                            HotKeyWorkerManager workerManager,
                                                            IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                                            IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        return new ScheduledPusherFactoryBean(properties, workerManager, hotKeyCollector, keyCountCollector);
    }

}
