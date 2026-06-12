package com.cloud.arch.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(RedisConfig.class)
public class RedissonAutoConfiguration {

    /**
     * Jackson序列化
     */
    @Bean
    @ConditionalOnMissingBean(Codec.class)
    public Codec codec() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.findAndRegisterModules();
        return new JsonJacksonCodec(objectMapper);
    }

    /**
     * 哨兵模式自动装配
     */
    @Bean
    @ConditionalOnProperty(
            name = {
                    "com.cloud.redis.sentinel.master-name", "com.cloud.redis.sentinel.sentinel-addresses"
            })
    public RedissonClient redissonSentinel(Codec codec, RedisConfig config) {
        return config.getSentinel().createClient(codec);
    }

    /**
     * 单例模式自动装配
     */
    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.redis.standalone", name = "address")
    public RedissonClient redissonSingle(Codec codec, RedisConfig config) {
        return config.getStandalone().createClient(codec);
    }

    /**
     * 集群模式自动装配
     */
    @Bean
    @ConditionalOnProperty(name = "com.cloud.redis.cluster.node-addresses")
    public RedissonClient redissonCluster(Codec codec, RedisConfig config) {
        return config.getCluster().createClient(codec);
    }

    /**
     * 主从模式自动装配
     */
    @Bean
    @ConditionalOnProperty(
            name = {"com.cloud.redis.master-slave.master-address", "com.cloud.redis.master-slave.slave-address"})
    public RedissonClient redissonMasterSlave(Codec codec, RedisConfig config) {
        return config.getMasterSlave().createClient(codec);
    }

    /**
     * 主从模式自动装配
     */
    @Bean
    @ConditionalOnProperty(name = "com.cloud.redis.replicated.address")
    public RedissonClient redissonReplicated(Codec codec, RedisConfig config) {
        return config.getReplicated().createClient(codec);
    }

    /**
     * 创建redisson模板类，提供常用方法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RedissonTemplate redissonTemplate(RedissonClient redissonClient) {
        return new RedissonTemplate(redissonClient);
    }

}
