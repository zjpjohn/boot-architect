package com.cloud.arch.mutex;

import org.redisson.api.RedissonClient;

public record DefaultMutexRedisSupplier(RedissonClient redissonClient) implements MutexRedisSupplier {

    @Override
    public RedissonClient get() {
        return this.redissonClient;
    }

}
