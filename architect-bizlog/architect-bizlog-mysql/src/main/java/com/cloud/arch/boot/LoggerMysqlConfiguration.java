package com.cloud.arch.boot;

import com.cloud.arch.repository.ILogQueryService;
import com.cloud.arch.repository.JdbcLogRepository;
import com.cloud.arch.service.JdbcLogQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureAfter(DataSource.class)
public class LoggerMysqlConfiguration {

    @Bean
    public JdbcLogRepository jdbcLogRepository(DataSource dataSource) {
        return new JdbcLogRepository(dataSource);
    }

    @Bean
    public ILogQueryService logQueryService(JdbcLogRepository jdbcLogRepository) {
        return new JdbcLogQueryService(jdbcLogRepository);
    }

}
