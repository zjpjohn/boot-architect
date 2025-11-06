package com.boot.architect.infrast.extension;

import com.cloud.arch.mutex.MutexRedisSupplier;
import com.cloud.arch.redis.RedisConfig;
import lombok.Getter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

@Getter
public class ExtMutexRedisSupplier implements MutexRedisSupplier {

    private final RedissonClient client;

    public ExtMutexRedisSupplier(RedisConfig.Standalone server, Codec codec) {
        this.client = server.createClient(codec);
    }

    @Override
    public RedissonClient get() {
        return this.client;
    }

}
