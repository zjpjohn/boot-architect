package com.cloud.arch.mutex.extension;

import com.cloud.arch.mutex.DefaultMutexRedisSupplier;
import com.cloud.arch.mutex.MutexRedisSupplier;
import com.cloud.arch.mutex.RedisContendControllerFactory;
import com.cloud.arch.mutex.core.ContendControllerFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Slf4j
@Configuration
@ConditionalOnClass(name = "com.cloud.arch.mutex.RedisContendControllerFactory")
public class RedisMutexConfiguration {

    /**
     * 默认使用redisson包自动配置的redissonTemplate,
     */
    @Bean
    @ConditionalOnMissingBean(MutexRedisSupplier.class)
    public MutexRedisSupplier redisSupplier(RedissonClient redissonClient) {
        return new DefaultMutexRedisSupplier(redissonClient);
    }

    @Bean
    @Primary
    @ConditionalOnBean(MutexRedisSupplier.class)
    public ContendControllerFactory controllerFactory(MutexRedisSupplier redisLoader) {
        log.info("using redis as contend controller...");
        return new RedisContendControllerFactory(redisLoader);
    }

}
