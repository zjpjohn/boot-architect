package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaffeineLocalCache extends AbstractLocalCache {

    private final Cache<Object, Object>    cache;
    private final ScheduledExecutorService scheduledExecutor;

    public CaffeineLocalCache(String name,
                              boolean allowNullValue,
                              LocalCacheSettings settings,
                              RefreshPolicy refreshPolicy,
                              AbstractRemoteCache remoteCache,
                              ScheduledExecutorService scheduledExecutor) {
        super(name, allowNullValue, settings, refreshPolicy, remoteCache);
        this.scheduledExecutor = scheduledExecutor;
        this.cache             = this.build(settings);
    }

    private Cache<Object, Object> build(LocalCacheSettings settings) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        caffeine.initialCapacity(settings.getInitialSize()).maximumSize(settings.getMaximumSize());
        if (scheduledExecutor != null) {
            Scheduler scheduler = Scheduler.forScheduledExecutorService(this.scheduledExecutor);
            caffeine.scheduler(scheduler);
        }
        if (ExpireMode.WRITE == settings.getExpireMode()) {
            caffeine.expireAfterWrite(settings.getExpireTime(), TimeUnit.SECONDS);
        } else {
            caffeine.expireAfterAccess(settings.getExpireTime(), TimeUnit.SECONDS);
        }
        caffeine.removalListener((key, value, cause) -> {
            if (log.isInfoEnabled()) {
                log.info("caffeine cache event action [{}],cache {name:{}, key:{}}", cause, this.getName(), key);
            }
        });
        return caffeine.build();
    }

    /**
     * 获取指定key的缓存value wrapper
     *
     * @param key 指定缓存key
     */
    @Override
    public Object doGet(Object key) {
        return this.cache.getIfPresent(key);
    }

    /**
     * 更新指定缓存key的本地缓存值value
     *
     * @param key   指定缓存key
     * @param value 更新的缓存值value
     */
    @Override
    public void doPut(Object key, Object value) {
        this.cache.put(key, value);
    }

    /**
     * 删除指定key的缓存值
     *
     * @param key 缓存key
     */
    @Override
    public void doEvict(Object key) {
        if (log.isInfoEnabled()) {
            log.info("evict caffeine cache[{}] key[{}]", this.getName(), key);
        }
        this.cache.invalidate(key);
    }

    /**
     * 清空本地缓存数据
     */
    @Override
    public void doClear() {
        if (log.isInfoEnabled()) {
            log.info("clear caffeine cache[{}] all values", this.getName());
        }
        this.cache.invalidateAll();
    }

    @Override
    public long cacheSize() {
        return this.cache.estimatedSize();
    }

}
