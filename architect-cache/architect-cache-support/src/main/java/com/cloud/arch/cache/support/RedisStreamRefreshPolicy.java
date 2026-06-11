package com.cloud.arch.cache.support;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;
import com.cloud.arch.cache.core.RefreshPolicy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamTrimArgs;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class RedisStreamRefreshPolicy implements RefreshPolicy {

    private final RStream<String, String> stream;
    private final CacheNodePolicy         cacheNodePolicy;
    private final int                     maxLen;

    public RedisStreamRefreshPolicy(String streamName, RedissonClient redissonClient, CacheNodePolicy cacheNodePolicy, int maxLen) {
        this.stream = redissonClient.getStream(streamName, StringCodec.INSTANCE);
        this.cacheNodePolicy = cacheNodePolicy;
        this.maxLen = maxLen;
    }

    @Override
    public void publish(RefreshEvent event) {
        try {
            String json = JSON.toJSONString(event);
            stream.add(StreamAddArgs.entry("event", json));
            stream.trimNonStrict(StreamTrimArgs.maxLen(maxLen).noLimit());
        } catch (Exception e) {
            log.warn("stream publish failed: {}", e.getMessage());
        }
    }

    @Override
    public long getRefreshNode() {
        return cacheNodePolicy.getCacheNode();
    }
}
