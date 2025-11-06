package com.cloud.arch.event.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@ConfigurationProperties(prefix = "com.cloud.event")
public class PublishEventProperties {

    /**
     * 生产者配置
     */
    private Publisher  publisher  = new Publisher();
    /**
     * 订阅者配置
     */
    private Subscriber subscriber = new Subscriber();

    @Data
    public static class Publisher {
        /**
         * 事件发布端，按需启动
         */
        private boolean enable                 = false;
        /**
         * 异步发送线程数
         */
        private Integer publishThreads         = 2;
        /**
         * 异步发送最大线程数
         */
        private Integer maxPublishThreads      = 8;
        /**
         * 异步发送缓存大小
         */
        private Integer publishCachedEventSize = 8192;
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

    @Data
    public static class Subscriber {
        /**
         * 时间订阅端，按需启动
         */
        private boolean        enable       = false;
        /**
         * 回收指定时间间隔之前的幂等信息
         */
        private Duration       before       = Duration.ofDays(2);
        /**
         * 回收延迟时间
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration       initialDelay = Duration.ofSeconds(10);
        /**
         * 回收间隔时间
         */
        @DurationUnit(ChronoUnit.DAYS)
        private Duration       period       = Duration.ofDays(4);
        /**
         * 回收分布式配置
         */
        private SchedulerMutex mutex        = new SchedulerMutex();
    }

}
