package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.AbstractRemoteCache;
import com.cloud.arch.cache.core.CacheEventListener;
import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

@Slf4j
public class RedisRefreshEventListener
        implements MessageListener<RefreshEvent>, CacheEventListener, SmartInitializingSingleton {

    private final String            topic;
    private final RedisCacheManager cacheManager;
    private final CacheNodePolicy   cacheNodePolicy;

    public RedisRefreshEventListener(String topic, RedisCacheManager cacheManager, CacheNodePolicy cacheNodePolicy) {
        Assert.notNull(topic, "event listener topic must not null.");
        this.topic           = topic;
        this.cacheManager    = cacheManager;
        this.cacheNodePolicy = cacheNodePolicy;
    }

    /**
     * 初始化事件监听器
     */
    @Override
    public void initialize() {
        RTopic publishTopic = cacheManager.getRedissonClient().getTopic(topic);
        publishTopic.addListener(RefreshEvent.class, this);
    }

    /**
     * 缓存事件监听节点编号
     */
    @Override
    public Long getLocalNode() {
        return cacheNodePolicy.getCacheNode();
    }

    @Override
    public void onMessage(CharSequence channel, RefreshEvent event) {
        this.onEvent(event);
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.initialize();
    }


    /**
     * 缓存操作事件监听器
     *
     * @param event 缓存操作事件
     */
    @Override
    public void onEvent(RefreshEvent event) {
        if (log.isInfoEnabled()) {
            log.info("cache refresh or evict event:{}", event);
        }
        AbstractRemoteCache remoteCache = (AbstractRemoteCache) cacheManager.getCache(event.getName());
        //本地事件或者未激活本地缓存忽略刷新事件
        if (this.canEvictLocalCache(remoteCache, event)) {
            return;
        }
        switch (event.getAction()) {
            case RefreshEvent.EVICT_KEY:
                remoteCache.getLocalCache().doEvict(event.getKey());
                break;
            case RefreshEvent.CLEAR_KEY:
                remoteCache.getLocalCache().doClear();
                break;
            default:
        }
    }

    /**
     * 内否淘汰本地缓存
     */
    private boolean canEvictLocalCache(AbstractRemoteCache remoteCache, RefreshEvent event) {
        return remoteCache == null || isLocalEvent(event) || !remoteCache.isActivatedLocal();
    }

}
