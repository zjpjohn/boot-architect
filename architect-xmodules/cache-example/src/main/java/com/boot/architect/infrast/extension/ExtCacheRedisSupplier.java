package com.boot.architect.infrast.extension;

import com.cloud.arch.cache.support.CacheRedisSupplier;
import com.cloud.arch.redis.RedisConfig;
import lombok.Getter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

/**
 * 自定义缓存redis配置
 */
@Getter
public class ExtCacheRedisSupplier implements CacheRedisSupplier {

    private final RedissonClient client;

    public ExtCacheRedisSupplier(RedisConfig.Standalone server, Codec codec) {
        this.client = server.createClient(codec);
    }

    @Override
    public RedissonClient get() {
        return this.client;
    }
}
