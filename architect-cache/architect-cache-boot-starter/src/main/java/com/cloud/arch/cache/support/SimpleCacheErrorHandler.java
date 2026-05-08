package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

@Slf4j
public class SimpleCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(Exception exception, Cache cache, Object key) {
        log.warn("缓存读取失败 cache[{}] key[{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(Exception exception, Cache cache, Object key, @Nullable Object value) {
        log.warn("缓存写入失败 cache[{}] key[{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(Exception exception, Cache cache, Object key) {
        log.warn("缓存淘汰失败 cache[{}] key[{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(Exception exception, Cache cache) {
        log.warn("缓存清空失败 cache[{}]: {}", cache.getName(), exception.getMessage());
    }
}
