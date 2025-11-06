package com.cloud.arch.cache.support;

import com.cloud.arch.redis.RedissonTemplate;
import org.redisson.api.RedissonClient;

public interface CacheRedisSupplier {

    /**
     * 缓存redis客户端bean加载器
     * 备注：当应用需要引入多个redis时，与缓存redis不同时需自定义redis提供
     */
    RedissonClient get();

}
