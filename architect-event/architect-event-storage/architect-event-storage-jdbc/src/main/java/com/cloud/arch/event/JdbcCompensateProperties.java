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
    private Integer  batch        = 20;
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
    private Duration initialDelay = Duration.ofSeconds(10);
    /**
     * 补偿间隔时间
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration period       = Duration.ofMinutes(2);

    /**
     * 补偿分布式配置
     */
    private SchedulerMutex mutex = new SchedulerMutex();

    /**
     * 成功事件清理配置
     */
    private CleanSucceed cleanSucceed = new CleanSucceed();

    /**
     * 死信归档配置
     */
    private DeadLetter deadLetter = new DeadLetter();


    @Data
    public static class CleanSucceed {
        /**
         * 成功事件保留天数，默认 7 天
         */
        private int            retainDays   = 7;
        /**
         * 每次清理批处理大小
         */
        private int            batchSize    = 1000;
        /**
         * 启动延迟时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration       initialDelay = Duration.ofSeconds(10);
        /**
         * 清理间隔时间，默认 1 小时
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration       period       = Duration.ofHours(1);
        /**
         * 清理分布式锁配置
         */
        private SchedulerMutex mutex        = new SchedulerMutex();
    }


    @Data
    public static class DeadLetter {
        /**
         * 归档批量
         */
        private int            batch        = 10;
        /**
         * 归档before时间前的事件
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration       before       = Duration.ofMinutes(1);
        /**
         * 归档range时间范围内的事件
         */
        @DurationUnit(ChronoUnit.DAYS)
        private Duration       range        = Duration.ofDays(7);
        /**
         * 启动延迟时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration       initialDelay = Duration.ofSeconds(30);
        /**
         * 归档间隔时间
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration       period       = Duration.ofMinutes(30);
        /**
         * 分布式锁配置
         */
        private SchedulerMutex mutex        = new SchedulerMutex();
    }

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
