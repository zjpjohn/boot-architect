package com.cloud.arch.mobile.boot;

import com.cloud.arch.mobile.sms.CloudSmsExecutor;
import com.cloud.arch.mobile.sms.CloudSmsProperties;
import com.cloud.arch.mobile.sms.SmsFlowController;
import com.cloud.arch.mobile.sms.impl.DefaultSmsFlowController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(CloudSmsProperties.class)
@ConditionalOnProperty(prefix = "com.cloud.sms", name = {"accessId", "secret"})
public class CloudSmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SmsFlowController.class)
    public SmsFlowController smsFlowControl() {
        return new DefaultSmsFlowController();
    }

    @Bean
    public CloudSmsExecutor cloudSmsExecutor(CloudSmsProperties properties, SmsFlowController smsFlowControl) {
        return new CloudSmsExecutor(properties, smsFlowControl);
    }

}
