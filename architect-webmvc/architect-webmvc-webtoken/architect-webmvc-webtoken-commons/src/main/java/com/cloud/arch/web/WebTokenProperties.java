package com.cloud.arch.web;

import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class WebTokenProperties {

    public static final String PROPS_PREFIX = "com.cloud.web.auth.token";

    /**
     * token加盐密钥
     */
    private String   secret;
    /**
     * token发布者
     */
    private String   issuer;
    /**
     * token过期时间
     */
    @DurationUnit(ChronoUnit.HOURS)
    private Duration expire = Duration.ofHours(48);
    /**
     * token header名称
     */
    private String   header = WebTokenConstants.AUTH_TOKEN_HEADER;
}
