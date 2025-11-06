package com.cloud.arch.oss.boot;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.cloud.arch.oss.props.OssCloudProperties;
import com.cloud.arch.oss.store.OssStorageTemplate;
import com.cloud.arch.oss.web.OssPolicyGenerator;
import com.cloud.arch.oss.web.UploadCallbackExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(OssCloudProperties.class)
@ConditionalOnProperty(prefix = "com.cloud.oss", name = {"appId", "endpoint", "secret", "bucket"})
public class ArchitectOssAutoConfiguration {

    @Bean
    public OSSClient ossClient(OssCloudProperties properties) {
        DefaultCredentialProvider provider
                = new DefaultCredentialProvider(properties.getAppId(), properties.getSecret());
        return new OSSClient(properties.getEndpoint(), provider, new ClientConfiguration());
    }

    @Bean
    public OssStorageTemplate storageTemplate(OssCloudProperties properties, OSSClient client) {
        return new OssStorageTemplate(client, properties);
    }

    @Configuration
    @EnableConfigurationProperties(OssCloudProperties.class)
    @ConditionalOnProperty(prefix = "com.cloud.oss.web-direct", name = "callback")
    public static class WebUploadConfiguration {

        @Bean
        public OssPolicyGenerator policyGenerator(OSSClient client, OssCloudProperties properties) {
            return new OssPolicyGenerator(client, properties);
        }

        @Bean
        public UploadCallbackExecutor callbackExecutor(OssCloudProperties properties) {
            return new UploadCallbackExecutor(properties);
        }

    }

}
