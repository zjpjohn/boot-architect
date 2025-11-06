package com.cloud.arch.boot;

import com.cloud.arch.interceptor.RptCheckInterceptor;
import com.cloud.arch.support.RptCheckRepository;
import com.cloud.arch.support.RptCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class RptCheckConfiguration {

    @Bean
    public RptCheckRepository existCheckService(DataSource dataSource) {
        return new RptCheckRepository(dataSource);
    }

    @Bean
    public RptCheckInterceptor existCheckInterceptor(RptCheckRepository existCheckService) {
        return new RptCheckInterceptor(existCheckService);
    }

    @Bean
    public RptCheckService rptCheckService(RptCheckRepository checkRepository) {
        return new RptCheckService(checkRepository);
    }

}
