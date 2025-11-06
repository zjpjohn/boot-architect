package com.cloud.arch.mutex.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContendMutexProps {

    /**
     * 初始延迟时间，默认-0秒
     */
    private Duration initialDelay = Duration.ofSeconds(0L);
    /**
     * 持有锁存活时间，默认-10秒
     */
    private Duration ttl          = Duration.ofSeconds(10L);
    /**
     * 持有锁优先续期延展时间，默认-6秒
     */
    private Duration transition   = Duration.ofSeconds(6L);

}
