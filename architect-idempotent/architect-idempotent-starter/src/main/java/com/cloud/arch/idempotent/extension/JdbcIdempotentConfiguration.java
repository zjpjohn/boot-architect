package com.cloud.arch.idempotent.extension;

import com.cloud.arch.idempotent.JdbcIdempotentManager;
import com.cloud.arch.idempotent.support.IdempotentManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableTransactionManagement
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.cloud.arch.idempotent.JdbcIdempotentManager")
public class JdbcIdempotentConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(DataSourceTransactionManager.class)
    public DataSourceTransactionManager txManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Primary
    @ConditionalOnBean(DataSource.class)
    public IdempotentManager idempotentManager(DataSource dataSource, DataSourceTransactionManager transactionManager) {
        return new JdbcIdempotentManager(dataSource, transactionManager);
    }

}
