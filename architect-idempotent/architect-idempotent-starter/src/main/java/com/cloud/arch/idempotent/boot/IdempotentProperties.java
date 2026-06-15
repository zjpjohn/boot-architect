package com.cloud.arch.idempotent.boot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.idempotent.mysql")
public class IdempotentProperties {

    /** 清理间隔（秒），默认 60 */
    private long cleanupInterval = 60L;

    /** 记录 TTL（秒），gmt_create 超过此时间的记录标记为过期，默认 120 */
    private long recordTtl = 120L;

}
