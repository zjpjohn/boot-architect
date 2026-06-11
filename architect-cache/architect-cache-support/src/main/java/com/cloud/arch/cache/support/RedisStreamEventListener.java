package com.cloud.arch.cache.support;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.cache.core.AbstractRemoteCache;
import com.cloud.arch.cache.core.CacheEventListener;
import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.time.Duration;
import java.util.Map;

@Slf4j
public class RedisStreamEventListener implements CacheEventListener, SmartInitializingSingleton {

    private final RStream<String, String> stream;
    private final RedisCacheManager       cacheManager;
    private final CacheNodePolicy         cacheNodePolicy;
    private final int                     batchSize;
    private final long                    blockTimeoutMs;

    private volatile boolean running = true;
    private          Thread  pollThread;

    public RedisStreamEventListener(String streamName, RedissonClient redissonClient, RedisCacheManager cacheManager, CacheNodePolicy cacheNodePolicy, int batchSize, long blockTimeoutMs) {
        this.stream = redissonClient.getStream(streamName, StringCodec.INSTANCE);
        this.cacheManager = cacheManager;
        this.cacheNodePolicy = cacheNodePolicy;
        this.batchSize = batchSize;
        this.blockTimeoutMs = blockTimeoutMs;
    }

    @Override
    public void initialize() {
        this.pollThread = new Thread(this::pollLoop, "cache-stream-poll");
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    private void pollLoop() {
        StreamMessageId lastId = StreamMessageId.NEWEST;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Map<StreamMessageId, Map<String, String>> messages = stream.read(StreamReadArgs.greaterThan(lastId)
                                                                                               .count(batchSize)
                                                                                               .timeout(Duration.ofMillis(blockTimeoutMs)));
                if (messages == null || messages.isEmpty()) {
                    continue;
                }
                for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
                    lastId = entry.getKey();
                    String json = entry.getValue().get("event");
                    if (json == null) {
                        continue;
                    }
                    RefreshEvent event = JSON.parseObject(json, RefreshEvent.class);
                    onEvent(event);
                }
            } catch (Exception e) {
                if (!running) break;
                log.error("stream poll error", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public void onEvent(RefreshEvent event) {
        if (log.isInfoEnabled()) {
            log.info("cache refresh or evict event:{}", event);
        }
        AbstractRemoteCache remoteCache = (AbstractRemoteCache) cacheManager.getCache(event.getName());
        if (remoteCache == null || isLocalEvent(event) || !remoteCache.isActivatedLocal()) {
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

    @Override
    public Long getLocalNode() {
        return cacheNodePolicy.getCacheNode();
    }

    @Override
    public void destroy() {
        this.running = false;
        if (this.pollThread != null) {
            this.pollThread.interrupt();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.initialize();
    }
}
