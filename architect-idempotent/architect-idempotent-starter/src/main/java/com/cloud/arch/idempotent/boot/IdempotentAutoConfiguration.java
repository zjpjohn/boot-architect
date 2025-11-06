package com.cloud.arch.idempotent.boot;

import com.cloud.arch.idempotent.aspect.IdempotentInterceptor;
import com.cloud.arch.idempotent.extension.JdbcIdempotentConfiguration;
import com.cloud.arch.idempotent.extension.RedisIdempotentConfiguration;
import com.cloud.arch.idempotent.support.IdempotentManager;
import com.cloud.arch.idempotent.support.IdempotentMetaContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableTransactionManagement
@AutoConfigureAfter(value = {JdbcIdempotentConfiguration.class, RedisIdempotentConfiguration.class})
public class IdempotentAutoConfiguration {

    @Bean
    public IdempotentMetaContainer idempotentMetaContainer() {
        return new IdempotentMetaContainer();
    }

    @Bean
    public IdempotentInterceptor idempotentInterceptor(IdempotentManager manager, IdempotentMetaContainer container) {
        return new IdempotentInterceptor(manager, container);
    }

}
