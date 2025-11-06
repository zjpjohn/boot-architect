package com.cloud.arch.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@ConfigurationProperties(prefix = "com.cloud.logger")
public class OperateLoggerProperties {

    /**
     * 是否开启异步存储日志
     */
    private Boolean  async       = false;
    /**
     * 异步核心线程数
     */
    private Integer  coreThreads = 1;
    /**
     * 异步最大线程数
     */
    private Integer  maxThreads  = 2;
    /**
     * 异步批量提交大小
     */
    private Integer  batchSize   = 20;
    /**
     * 异步批量等待超时时间(单位-秒)
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout     = Duration.ofSeconds(5);

}
