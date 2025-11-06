package com.cloud.arch.mutex.schedule;

import lombok.Getter;

import java.time.Duration;

@Getter
public class SchedulerConfig {

    private final ScheduleType type;
    private final Duration     initialDelay;
    private final Duration     period;

    public SchedulerConfig(ScheduleType type, Duration initialDelay, Duration period) {
        this.type         = type;
        this.initialDelay = initialDelay;
        this.period       = period;
    }

    public static SchedulerConfig ofRate(Duration initialDelay, Duration period) {
        return new SchedulerConfig(ScheduleType.FIXED_RATE, initialDelay, period);
    }

    public static SchedulerConfig ofDelay(Duration initialDelay, Duration period) {
        return new SchedulerConfig(ScheduleType.FIXED_DELAY, initialDelay, period);
    }

}

