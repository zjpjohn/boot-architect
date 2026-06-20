package com.cloud.arch.event.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * 领域事件配置属性，绑定 {@code com.cloud.event} 前缀，包含发布端线程池和订阅端清理调度参数。
 */
@Data
@ConfigurationProperties(prefix = "com.cloud.event")
public class EventProperties {

    /**
     * 是否开启指标采集
     */
    private Metric    metric    = new Metric();
    /**
     * 生产者配置
     */
    private Publisher publisher = new Publisher();

    /**
     * 订阅者配置
     */
    private Subscriber subscriber = new Subscriber();

    @Data
    public static class Metric {
        /**
         * 是否启用 Micrometer 指标采集，默认 false
         */
        private boolean enabled = false;
    }

    @Data
    public static class Publisher {
        /**
         * 事件发布端，按需启动
         */
        private boolean enable = false;
        /**
         * 批量标记参数
         */
        private Marker  marker = new Marker();
        /**
         * 攒批发送配置
         */
        private Batch   batch  = new Batch();

    }

    @Data
    public static class Marker {
        /**
         * 单次 batchUpdate 最大条数
         */
        private int  batchSize = 100;
        /**
         * 窃取刷新间隔(ms)
         */
        private long interval  = 200;
    }

    @Data
    public static class Batch {
        /**
         * 每次 drain 最大条数
         */
        private int  batchSize     = 20;
        /**
         * drain 等待超时(ms)
         */
        private long drainTimeout  = 200;
        /**
         * 内存队列容量
         */
        private int  queueCapacity = 65536;
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
