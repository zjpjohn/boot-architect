package com.cloud.arch.mobile.boot;

import com.cloud.arch.mobile.sms.CloudSmsExecutor;
import com.cloud.arch.mobile.sms.CloudSmsProperties;
import com.cloud.arch.mobile.sms.SmsFlowController;
import com.cloud.arch.mobile.sms.impl.DefaultSmsFlowController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableConfigurationProperties(CloudSmsProperties.class)
@ConditionalOnProperty(prefix = "com.cloud.sms", name = {"accessId", "secret"})
public class CloudSmsAutoConfiguration {

    public static final String SMS_EXECUTOR_SERVICE_NAME = "sms-send-executor-name";

    @Bean
    @ConditionalOnMissingBean(SmsFlowController.class)
    public SmsFlowController smsFlowControl() {
        return new DefaultSmsFlowController();
    }

    @Bean(name = SMS_EXECUTOR_SERVICE_NAME)
    public Executor smsSendExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1024);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("sms-send-thread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public CloudSmsExecutor cloudSmsExecutor(
            @Qualifier(SMS_EXECUTOR_SERVICE_NAME) Executor smsSendExecutor,
            CloudSmsProperties properties,
            SmsFlowController smsFlowControl) {
        return new CloudSmsExecutor(smsSendExecutor, properties, smsFlowControl);
    }

}
