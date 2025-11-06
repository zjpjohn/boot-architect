package com.cloud.arch.transaction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@ConfigurationProperties(prefix = "com.cloud.async.transaction")
public class AsyncTaskProperties {
    /**
     * 补偿发送批量大小
     */
    private Integer        batch        = 50;
    /**
     * 启动延迟时间
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration       initialDelay = Duration.ofSeconds(10);
    /**
     * 补偿间隔时间
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration       period       = Duration.ofSeconds(60);
    /**
     * 补偿分布式配置
     */
    private SchedulerMutex mutex        = new SchedulerMutex();
    /**
     * 异步执行任务线程池配置
     */
    private AsyncExecutor  business     = new AsyncExecutor();
    /**
     * 重试任务线程池配置
     */
    private RetryExecutor  retry        = new RetryExecutor();

    /**
     * 异步执行任务线程池配置
     */
    @Data
    public static class AsyncExecutor {
        /**
         * 核心线程数
         */
        private Integer core      = 2;
        /**
         * 最大线程数
         */
        private Integer maxSize   = 32;
        /**
         * 线程活跃时间
         */
        private Integer keepAlive = 600;
        /**
         * 缓冲队列大小
         */
        private Integer queueSize = 4096;
    }

    /**
     * 补偿任务线程池配置
     */
    @Data
    public static class RetryExecutor {
        /**
         * 核心线程数
         */
        private Integer core      = 1;
        /**
         * 最大线程数
         */
        private Integer maxSize   = 4;
        /**
         * 线程活跃时间
         */
        private Integer keepAlive = 300;
        /**
         * 缓冲队列大小
         */
        private Integer queueSize = 1024;
    }

    @Data
    public static class SchedulerMutex {
        /**
         * 初始延迟时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration initialDelay = Duration.ofSeconds(5);
        /**
         * 所过期时间
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
