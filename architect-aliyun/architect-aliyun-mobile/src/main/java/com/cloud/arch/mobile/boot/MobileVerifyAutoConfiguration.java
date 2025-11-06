package com.cloud.arch.mobile.boot;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.cloud.arch.mobile.verify.DypnsApiProperties;
import com.cloud.arch.mobile.verify.GetMobileExecutor;
import com.cloud.arch.mobile.verify.VerifyMobileExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(DypnsApiProperties.class)
@ConditionalOnProperty(prefix = "com.cloud.mobile", name = {"accessKey", "accessSecret"})
public class MobileVerifyAutoConfiguration {

    @Bean
    public Client dypnsApiClient(DypnsApiProperties properties) throws Exception {
        Config conf = new Config().setAccessKeyId(properties.getAccessKey())
                                  .setAccessKeySecret(properties.getAccessSecret())
                                  .setEndpoint(properties.getEndpoint());
        return new Client(conf);
    }

    @Bean
    public GetMobileExecutor getMobileExecutor(Client client) {
        return new GetMobileExecutor(client);
    }

    @Bean
    public VerifyMobileExecutor verifyMobileExecutor(Client client) {
        return new VerifyMobileExecutor(client);
    }

}
