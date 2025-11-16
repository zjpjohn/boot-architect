package com.cloud.arch.rocket.consumer.spring;

import com.cloud.arch.rocket.commons.BaseOnsConfiguration;
import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.consumer.core.IdempotentCleanScheduler;
import com.cloud.arch.rocket.idempotent.Idempotent;
import com.cloud.arch.rocket.idempotent.IdempotentCleanHandler;
import com.cloud.arch.rocket.idempotent.impl.JdbcIdempotentChecker;
import com.cloud.arch.rocket.idempotent.impl.TransactionIdempotentChecker;
import com.cloud.arch.rocket.serializable.JsonSerialize;
import com.cloud.arch.rocket.serializable.Serialize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(RocketmqProperties.class)
@ConditionalOnProperty(prefix = "com.cloud.rocket.v4x.consumer", name = "enable", havingValue = "true")
public class ConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Serialize.class)
    public JsonSerialize serialize() {
        return new JsonSerialize();
    }

    @Bean
    public RocketmqConsumerProcessor consumerProcessor(RocketmqProperties properties, Serialize serialize) {
        return new RocketmqConsumerProcessor(properties, serialize);
    }


    @Configuration
    @EnableConfigurationProperties(RocketmqProperties.class)
    @ConditionalOnProperty(value = "com.cloud.rocket.v4x.consumer.idempotent")
    @AutoConfigureOrder(Integer.MAX_VALUE)
    @EnableTransactionManagement
    public static class IdempotentAutoConfiguration extends BaseOnsConfiguration {

        @Bean
        @ConditionalOnMissingBean(DataSourceTransactionManager.class)
        public DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean(name = Idempotent.JDBC_IDEMPOTENT_CHECK)
        public JdbcIdempotentChecker jdbcIdempotentChecker(DataSource dataSource) {
            return new JdbcIdempotentChecker(dataSource);
        }

        @Bean(name = Idempotent.JDBC_TRANSACTION_IDEMPOTENT_CHECK)
        public TransactionIdempotentChecker transactionIdempotentChecker(DataSourceTransactionManager transactionManager) {
            return new TransactionIdempotentChecker(transactionManager);
        }

        @Bean
        @ConditionalOnBean(JdbcIdempotentChecker.class)
        public IdempotentCleanHandler garbageHandler(JdbcIdempotentChecker jdbcIdempotentChecker) {
            return new IdempotentCleanHandler(jdbcIdempotentChecker);
        }

        @Bean
        @ConditionalOnBean(IdempotentCleanHandler.class)
        public IdempotentCleanScheduler garbageScheduler(IdempotentCleanHandler garbageHandler,
                                                         RocketmqProperties queueProperties) {
            return new IdempotentCleanScheduler(garbageHandler, queueProperties);
        }

    }

}
