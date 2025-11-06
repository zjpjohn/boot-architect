package com.cloud.arch.operate.boot;

import com.cloud.arch.Ip2RegionSearcher;
import com.cloud.arch.operate.aspect.OperationLogAspect;
import com.cloud.arch.operate.core.*;
import com.cloud.arch.operate.props.OperateLogProperties;
import com.cloud.arch.operate.repository.LogJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableConfigurationProperties(OperateLogProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class OperateLogConfiguration {

    @Bean
    public LogJdbcRepository logJdbcRepository(DataSource dataSource) {
        return new LogJdbcRepository(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(Ip2RegionSearcher.class)
    public Ip2RegionSearcher ip2RegionSearcher() {
        return new Ip2RegionSearcher();
    }

    @Bean
    @ConditionalOnMissingBean(IOperatorResolver.class)
    public IOperatorResolver operatorResolver() {
        return new DefaultOperatorResolver();
    }

    @Bean
    public OperationLogHandle operationLogHandle(OperateLogProperties properties,
                                                 LogJdbcRepository logJdbcRepository,
                                                 Ip2RegionSearcher ipRegionSearcher,
                                                 IOperatorResolver operatorResolver) {
        return new OperationLogHandle(logJdbcRepository, properties, ipRegionSearcher, operatorResolver);
    }

    @Bean
    public AsyncLogDispatcher asyncLogDispatcher(OperateLogProperties properties,
                                                 OperationLogHandle operationLogHandle) {
        return new AsyncLogDispatcher(properties, operationLogHandle);
    }

    @Bean
    public AspectHandleSupport handleSupport(AsyncLogDispatcher asyncLogDispatcher) {
        return new AspectHandleSupport(asyncLogDispatcher);
    }

    @Bean
    public OperationLogAspect operationLogAspect(AspectHandleSupport aspectHandleSupport) {
        return new OperationLogAspect(aspectHandleSupport);
    }

}
