package com.cloud.arch.event;

import com.cloud.arch.event.remoting.RemotingProperties;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class RocksCompensateProperties {

    public static final String DEFAULT_DB_PATH = "/event/storage";

    /**
     * rocksdb事件存放路径
     */
    private String             eventPath      = DEFAULT_DB_PATH;
    /**
     * 事件补偿服务器
     */
    private String             servers;
    /**
     * 请求appKey
     */
    private String             appKey;
    /**
     * 请求accessToken
     */
    private String             accessToken;
    /**
     * 补偿发送批量
     */
    private Integer            batch          = 20;
    /**
     * 补偿推送线程
     */
    private Integer            pushThreads    = 1;
    /**
     * 补偿推送最大线程
     */
    private Integer            pushMaxThreads = 3;
    /**
     * 补偿推送缓存队列大小
     */
    private Integer            pushQueueSize  = 512;
    /**
     * 补偿发送before时间前的事件
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration           before         = Duration.ofSeconds(5);
    /**
     * 补偿发送初始延迟(单位:秒)
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration           initialDelay   = Duration.ofSeconds(30);
    /**
     * 补偿发送时间间隔(单位:秒)
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration           period         = Duration.ofSeconds(10);
    /**
     * http请求配置信息
     */
    @NestedConfigurationProperty
    private RemotingProperties remoting       = new RemotingProperties();

}
