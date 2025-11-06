package com.boot.architect.infrast.config;

import com.boot.architect.infrast.extension.ExtMutexRedisSupplier;
import com.cloud.arch.mutex.MutexRedisSupplier;
import com.cloud.arch.redis.RedisConfig;
import org.redisson.client.codec.Codec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class MutexRedisConfigurer {
//
//    @Bean
//    @ConfigurationProperties(prefix = "com.cloud.mutex.redis")
//    public RedisConfig.Standalone standalone() {
//        return new RedisConfig.Standalone();
//    }
//
//    @Bean
//    public MutexRedisSupplier redisLoader(RedisConfig.Standalone server, Codec codec) {
//        return new ExtMutexRedisSupplier(server, codec);
//    }
//
//}
