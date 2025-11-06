package com.cloud.arch.idempotent.extension;

import com.cloud.arch.idempotent.DefaultRedisSupplier;
import com.cloud.arch.idempotent.IdemRedisSupplier;
import com.cloud.arch.idempotent.RedisIdempotentManager;
import com.cloud.arch.idempotent.support.IdempotentManager;
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
@ConditionalOnClass(name = "com.cloud.arch.idempotent.RedisIdempotentManager")
public class RedisIdempotentConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdemRedisSupplier.class)
    public IdemRedisSupplier redisSupplier(RedissonClient redissonClient) {
        return new DefaultRedisSupplier(redissonClient);
    }

    @Bean
    @Primary
    @ConditionalOnBean(IdemRedisSupplier.class)
    public IdempotentManager idempotentManager(IdemRedisSupplier supplier) {
        return new RedisIdempotentManager(supplier.get());
    }

}
