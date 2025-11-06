package com.cloud.arch.operate.props;

import com.google.common.base.Splitter;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "com.cloud.operate")
public class OperateLogProperties {

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
    private Duration timeout     = Duration.ofSeconds(2);
    /**
     * 全局统一提出保存敏感字段
     */
    private String   excludes    = "";

    public List<String> excludeList() {
        return Splitter.on(",").trimResults().splitToList(this.excludes);
    }

}
