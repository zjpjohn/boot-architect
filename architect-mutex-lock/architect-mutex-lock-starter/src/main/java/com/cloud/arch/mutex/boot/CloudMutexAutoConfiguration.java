package com.cloud.arch.mutex.boot;

import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendControllerFactory;
import com.cloud.arch.mutex.extension.JdbcMutexConfiguration;
import com.cloud.arch.mutex.extension.RedisMutexConfiguration;
import com.cloud.arch.mutex.utils.Threads;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureAfter(value = {JdbcMutexConfiguration.class, RedisMutexConfiguration.class})
public class CloudMutexAutoConfiguration {

    public static final String MUTEX_SCHEDULE_EXECUTOR_BEAN = "mutex-schedule-executor-bean";

    @Bean(name = MUTEX_SCHEDULE_EXECUTOR_BEAN)
    public ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(Runtime.getRuntime()
                                                      .availableProcessors(), Threads.threadFactory("mutex-schedule-"));
    }

    @Bean
    public MutexTemplate mutexTemplate(
            @Qualifier(value = MUTEX_SCHEDULE_EXECUTOR_BEAN) ScheduledExecutorService scheduledExecutorService,
            ContendControllerFactory controllerFactory) {
        return new MutexTemplate(scheduledExecutorService, controllerFactory);
    }

}
