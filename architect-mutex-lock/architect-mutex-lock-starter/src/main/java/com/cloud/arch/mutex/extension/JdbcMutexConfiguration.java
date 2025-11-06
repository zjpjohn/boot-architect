package com.cloud.arch.mutex.extension;

import com.cloud.arch.mutex.IMutexOwnerRepository;
import com.cloud.arch.mutex.JdbcContendControllerFactory;
import com.cloud.arch.mutex.MutexOwnerRepository;
import com.cloud.arch.mutex.core.ContendControllerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.cloud.arch.mutex.JdbcContendControllerFactory")
public class JdbcMutexConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public IMutexOwnerRepository mutexOwnerRepository(DataSource dataSource) {
        return new MutexOwnerRepository(dataSource);
    }

    @Bean
    @Primary
    public ContendControllerFactory controllerFactory(IMutexOwnerRepository mutexOwnerRepository) {
        log.info("using mysql as contend controller...");
        return new JdbcContendControllerFactory(mutexOwnerRepository);
    }

}
