package com.cloud.arch.idempotent;

import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class IdempotentProperties {

    /**
     * 清理间隔（秒），默认 60
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration       interval     = Duration.ofSeconds(60);
    /**
     * 记录 gmt_create 超过此时间的记录标记为过期，默认 30
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration       expire       = Duration.ofSeconds(30);
    /**
     * 初始清理延迟时间，默认30秒
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration       initialDelay = Duration.ofSeconds(30);
    /**
     * 分布式锁
     */
    private SchedulerMutex mutex        = new SchedulerMutex();

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
