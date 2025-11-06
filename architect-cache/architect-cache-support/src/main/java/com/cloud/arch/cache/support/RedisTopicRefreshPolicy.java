package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;
import com.cloud.arch.cache.core.RefreshPolicy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.util.Assert;

@Slf4j
public class RedisTopicRefreshPolicy implements RefreshPolicy {

    private final RTopic          publishTopic;
    private final CacheNodePolicy cacheNodePolicy;

    public RedisTopicRefreshPolicy(String topic, RedissonClient redissonClient, CacheNodePolicy cacheNodePolicy) {
        Assert.notNull(topic, "event listener topic must not null.");
        this.cacheNodePolicy = cacheNodePolicy;
        this.publishTopic    = redissonClient.getTopic(topic);
    }

    /**
     * 发布刷新缓存事件
     *
     * @param event 缓存事件
     */
    @Override
    public void publish(RefreshEvent event) {
        this.publishTopic.publish(event);
    }

    /**
     * 获取刷新节点编号，判断是否为本地时间
     */
    @Override
    public long getRefreshNode() {
        return cacheNodePolicy.getCacheNode();
    }

}
