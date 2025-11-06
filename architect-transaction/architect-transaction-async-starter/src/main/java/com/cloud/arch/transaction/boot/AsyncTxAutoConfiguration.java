package com.cloud.arch.transaction.boot;

import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.boot.CloudMutexAutoConfiguration;
import com.cloud.arch.transaction.codec.AsyncEventCodec;
import com.cloud.arch.transaction.codec.JsonEventCodec;
import com.cloud.arch.transaction.config.AsyncTaskProperties;
import com.cloud.arch.transaction.core.AsyncTxExecutor;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import com.cloud.arch.transaction.interceptor.AsyncTransactionInterceptor;
import com.cloud.arch.transaction.support.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableTransactionManagement
@EnableConfigurationProperties(AsyncTaskProperties.class)
@AutoConfigureAfter(value = {DataSourceAutoConfiguration.class, CloudMutexAutoConfiguration.class})
public class AsyncTxAutoConfiguration {

    private static final String ASYNC_RETRY_EXECUTOR    = "async-retry-executor";
    private static final String ASYNC_BUSINESS_EXECUTOR = "async-business-executor";

    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager txManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(AsyncEventCodec.class)
    public AsyncEventCodec asyncEventCodec() {
        return new JsonEventCodec();
    }

    @Bean
    public IAsyncTxRepository asyncTxRepository(DataSource dataSource, AsyncEventCodec asyncEventCodec) {
        return new JdbcAsyncTxRepository(dataSource, asyncEventCodec);
    }

    @Bean
    public AsyncTransactionInterceptor asyncTransactionInterceptor() {
        return new AsyncTransactionInterceptor();
    }

    @Bean
    public AsyncTxInvokerProcessor asyncTxInvokerProcessor(PlatformTransactionManager transactionManager,
                                                           TransactionAttributeSource transactionAttributeSource) {
        return new AsyncTxInvokerProcessor(transactionManager, transactionAttributeSource);
    }

    @Bean(name = ASYNC_BUSINESS_EXECUTOR)
    public Executor businessExecutor(AsyncTaskProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getBusiness().getCore());
        executor.setMaxPoolSize(properties.getBusiness().getMaxSize());
        executor.setQueueCapacity(properties.getBusiness().getQueueSize());
        executor.setKeepAliveSeconds(properties.getBusiness().getKeepAlive());
        executor.setThreadNamePrefix("async-tx-business-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = ASYNC_RETRY_EXECUTOR)
    public Executor retryExecutor(AsyncTaskProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getRetry().getCore());
        executor.setMaxPoolSize(properties.getRetry().getMaxSize());
        executor.setQueueCapacity(properties.getRetry().getQueueSize());
        executor.setKeepAliveSeconds(properties.getRetry().getKeepAlive());
        executor.setThreadNamePrefix("async-tx-retry-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncTxExecutor asyncTxExecutor(@Qualifier(ASYNC_BUSINESS_EXECUTOR) Executor executor,
                                           IAsyncTxRepository asyncTxRepository) {
        return new AsyncTxExecutor(executor, asyncTxRepository);
    }

    @Bean
    public AsyncTxSynchronization asyncTxSynchronization(AsyncTxExecutor asyncTxExecutor,
                                                         IAsyncTxRepository asyncTxRepository) {
        return new AsyncTxSynchronization(asyncTxExecutor, asyncTxRepository);
    }

    @Bean
    public AsyncRetryQueue asyncRetryQueue(@Qualifier(ASYNC_RETRY_EXECUTOR) Executor executor,
                                           IAsyncTxRepository asyncTxRepository) {
        return new AsyncRetryQueue(executor, asyncTxRepository);
    }

    @Bean
    public AsyncCompensateScheduler asyncCompensateScheduler(MutexTemplate mutexTemplate,
                                                             IAsyncTxRepository asyncTxRepository,
                                                             AsyncRetryQueue asyncRetryQueue,
                                                             AsyncTaskProperties properties) {
        return new AsyncCompensateScheduler(mutexTemplate, asyncTxRepository, asyncRetryQueue, properties);
    }

    @Bean
    public AsyncReparationScheduler asyncReparationScheduler(MutexTemplate mutexTemplate,
                                                             IAsyncTxRepository asyncTxRepository,
                                                             AsyncTxExecutor asyncExecutor) {
        return new AsyncReparationScheduler(mutexTemplate, asyncTxRepository, asyncExecutor);
    }

}
