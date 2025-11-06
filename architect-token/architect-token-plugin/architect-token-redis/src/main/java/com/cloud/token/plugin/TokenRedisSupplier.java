package com.cloud.token.plugin;

import org.redisson.api.RedissonClient;

public interface TokenRedisSupplier {
    /**
     * redis客户端bean加载
     * 备注:当应用需要多个redis应用时，与互斥锁redis不同时需自定义redis提供
     */
    RedissonClient get();

}
