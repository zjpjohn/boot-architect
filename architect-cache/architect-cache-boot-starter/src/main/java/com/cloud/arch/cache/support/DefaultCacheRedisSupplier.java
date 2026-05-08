package com.cloud.arch.cache.support;

import org.redisson.api.RedissonClient;

/**
 * 默认 Redis 客户端供应器，直接从 Spring 容器中注入的 {@link RedissonClient} 获取 Redis 连接
 */
public record DefaultCacheRedisSupplier(RedissonClient redissonClient) implements CacheRedisSupplier {

    @Override
    public RedissonClient get() {
        return this.redissonClient;
    }

}
