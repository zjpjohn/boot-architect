package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.*;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaffeineLocalCache extends AbstractLocalCache {

    private final LoadingCache<Object, Object> cache;
    private final ScheduledExecutorService     scheduledExecutor;

    public CaffeineLocalCache(String name, boolean allowNullValue, LocalCacheSettings settings, RefreshPolicy refreshPolicy, AbstractRemoteCache remoteCache, ScheduledExecutorService scheduledExecutor, int maxLocalTtlSeconds) {
        super(name, allowNullValue, settings, refreshPolicy, remoteCache);
        this.scheduledExecutor = scheduledExecutor;
        this.cache = this.build(settings, remoteCache, maxLocalTtlSeconds);
    }

    private LoadingCache<Object, Object> build(LocalCacheSettings settings, AbstractRemoteCache remoteCache, int maxLocalTtlSeconds) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                                                    .initialCapacity(settings.getInitialSize())
                                                    .maximumSize(settings.getMaximumSize());
        if (scheduledExecutor != null) {
            caffeine.scheduler(Scheduler.forScheduledExecutorService(this.scheduledExecutor));
        }
        long effectiveTtl = settings.getExpireTime();
        if (maxLocalTtlSeconds > 0) {
            effectiveTtl = Math.min(effectiveTtl, maxLocalTtlSeconds);
        }
        if (ExpireMode.WRITE == settings.getExpireMode()) {
            caffeine.expireAfterWrite(effectiveTtl, TimeUnit.SECONDS);
        } else {
            caffeine.expireAfterAccess(effectiveTtl, TimeUnit.SECONDS);
        }
        caffeine.removalListener((key, value, cause) -> {
            if (log.isInfoEnabled()) {
                log.info("caffeine cache event action [{}],cache {name:{}, key:{}}", cause, this.getName(), key);
            }
        });
        return caffeine.build(key -> remoteCache.doGet(key));
    }

    @Override
    public <T> T get(Object key) {
        Object value = this.cache.getIfPresent(key);
        if (value != null) {
            this.remoteCache.statsCounter().recordHits(1, true);
            return (T) toValue(value);
        }
        // L1 miss → LoadingCache.get() 触发 loader，L2 指标由 doGet 内部记录
        value = this.cache.get(key);
        return (T) toValue(value);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = this.cache.getIfPresent(key);
        if (value != null) {
            this.remoteCache.statsCounter().recordHits(1, true);
            return (T) toValue(value);
        }
        value = this.cache.get(key, k -> {
            try {
                Object userValue = valueLoader.call();
                return toStoreValue(userValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return (T) toValue(value);
    }

    @Override
    public Object doGet(Object key) {
        return this.cache.get(key);
    }

    @Override
    public void doPut(Object key, Object value) {
        this.cache.put(key, value);
    }

    @Override
    public void doEvict(Object key) {
        if (log.isInfoEnabled()) {
            log.info("evict caffeine cache[{}] key[{}]", this.getName(), key);
        }
        this.cache.invalidate(key);
    }

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
