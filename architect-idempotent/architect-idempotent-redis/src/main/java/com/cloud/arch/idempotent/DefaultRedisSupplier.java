package com.cloud.arch.idempotent;

import org.redisson.api.RedissonClient;

/**
 * 默认Redis客户端提供者，集成Redisson自动配置
 */
public record DefaultRedisSupplier(RedissonClient redissonClient) implements IdemRedisSupplier {

    @Override
    public RedissonClient get() {
        return this.redissonClient;
    }

}
