package com.cloud.arch.cache.extension;

import com.cloud.arch.cache.core.CacheEventListener;
import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;
import com.cloud.arch.hotkey.core.key.HotKeyCache;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.springframework.beans.factory.SmartInitializingSingleton;

public class HotKeyRefreshEventListener
        implements MessageListener<RefreshEvent>, CacheEventListener, SmartInitializingSingleton {


    private final String             topic;
    private final HotKeyCacheManager cacheManager;
    private final CacheNodePolicy    cacheNodePolicy;

    public HotKeyRefreshEventListener(String topic, HotKeyCacheManager cacheManager, CacheNodePolicy cacheNodePolicy) {
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
     * 缓存操作事件监听器
     *
     * @param event 缓存操作事件
     */
    @Override
    public void onEvent(RefreshEvent event) {
        if (isLocalEvent(event)) {
            return;
        }
        HotKeyCache hotKeyCache = cacheManager.getHotKeyCache();
        switch (event.getAction()) {
            case RefreshEvent.EVICT_KEY:
                hotKeyCache.remove(event.getName(), event.getKey().toString());
                break;
            case RefreshEvent.CLEAR_KEY:
                hotKeyCache.removeAll(event.getName());
                break;
            default:
        }
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
}
