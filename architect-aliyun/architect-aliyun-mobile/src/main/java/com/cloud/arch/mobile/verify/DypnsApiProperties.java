package com.cloud.arch.mobile.verify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.mobile")
public class DypnsApiProperties {

    private String accessKey;
    private String accessSecret;
    private String endpoint = "dypnsapi.aliyuncs.com";
}
