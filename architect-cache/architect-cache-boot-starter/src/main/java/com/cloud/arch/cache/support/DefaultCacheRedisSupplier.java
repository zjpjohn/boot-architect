package com.cloud.arch.cache.support;

import org.redisson.api.RedissonClient;

public record DefaultCacheRedisSupplier(RedissonClient redissonClient) implements CacheRedisSupplier {

    @Override
    public RedissonClient get() {
        return this.redissonClient;
    }

}
