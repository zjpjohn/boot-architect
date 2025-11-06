package com.cloud.arch.mobile.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.sms")
public class CloudSmsProperties {

    /**
     * sms srv accessId
     */
    private String accessId;
    /**
     * sms srv secret
     */
    private String secret;
    /**
     * sms product: Dysmsapi
     */
    private String product  = "Dysmsapi";
    /**
     * sms region: cn-hangzhou
     */
    private String region   = "cn-hangzhou";
    /**
     * sms endpoint: dysmsapi.aliyuncs.com
     */
    private String endpoint = "dysmsapi.aliyuncs.com";

}
