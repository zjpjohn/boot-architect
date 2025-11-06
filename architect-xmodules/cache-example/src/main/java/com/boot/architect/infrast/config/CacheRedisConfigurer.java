package com.boot.architect.infrast.config;

import com.boot.architect.infrast.extension.ExtCacheRedisSupplier;
import com.cloud.arch.cache.support.CacheRedisSupplier;
import com.cloud.arch.redis.RedisConfig;
import org.redisson.client.codec.Codec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheRedisConfigurer {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.cache.redis")
    public RedisConfig.Standalone redisServer() {
        return new RedisConfig.Standalone();
    }

    @Bean
    public CacheRedisSupplier redisSupplier(RedisConfig.Standalone server, Codec codec) {
        return new ExtCacheRedisSupplier(server, codec);
    }

}
