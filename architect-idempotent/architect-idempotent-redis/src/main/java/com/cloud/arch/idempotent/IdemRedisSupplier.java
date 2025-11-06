package com.cloud.arch.idempotent;

import org.redisson.api.RedissonClient;

/**
 * redis客户端提供者
 */
public interface IdemRedisSupplier {

    RedissonClient get();

}
