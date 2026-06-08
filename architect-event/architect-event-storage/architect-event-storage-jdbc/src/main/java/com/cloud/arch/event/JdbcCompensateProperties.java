package com.cloud.arch.event;

import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class JdbcCompensateProperties {
    /**
     * 补偿发送最大版本号
     */
    private Integer  maxVersion   = 10;
    /**
     * 补偿发送批量
     */
    private Integer  batch        = 10;
    /**
     * 补偿发送before时间前的事件
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration before       = Duration.ofMinutes(1);
    /**
     * 补偿发送range时间范围内的事件
     */
    @DurationUnit(ChronoUnit.DAYS)
    private Duration range        = Duration.ofDays(7);
    /**
     * 启动延迟时间
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration initialDelay = Duration.ofSeconds(5);
    /**
     * 补偿间隔时间
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration period       = Duration.ofMinutes(5);
    /**
     * 批量标记配置
     */
    private Marker   marker       = new Marker();

    @Data
    public static class Marker {
        /**
         * 单次 batchUpdate 最大条数
         */
        private int  maxBatchSize  = 500;
        /**
         * 窃取刷新间隔(ms)
         */
        private long stealInterval = 500;
    }

    /**
     * 补偿分布式配置
     */
    private SchedulerMutex mutex = new SchedulerMutex();

    @Data
    public static class SchedulerMutex {
        /**
         * 初始延迟时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration initialDelay = Duration.ofSeconds(5);
        /**
         * 锁过期时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration ttl          = Duration.ofSeconds(30);
        /**
         * 锁续期时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration transition   = Duration.ofSeconds(15);
    }

}
