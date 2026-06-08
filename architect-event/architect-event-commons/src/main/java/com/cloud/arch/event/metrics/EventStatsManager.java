package com.cloud.arch.event.metrics;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 事件运营指标管理器，参考 {@code architect-cache} 的 {@code StatsManager} 模式。
 * <p>
 * 作为工厂为每个 topic 创建独立的 {@link EventStatsCounter}，
 * 同时承载全局性指标（补偿周期、批量标记刷盘等无队列维度的指标）。
 * <p>
 * 所有方法均为 default no-op，禁用态由 {@link #disabled()} 返回的空枚举提供。
 */
public interface EventStatsManager {

    static EventStatsManager disabled() {
        return DisabledStatsManager.INSTANCE;
    }

    default EventStatsCounter statsCounter(String topic) {
        return EventStatsCounter.disabled();
    }

    default void incrementCompensateCycle() {
    }

    default void incrementCompensateRetry(int count) {
    }

    default void incrementCompensateDeadLetter(int count) {
    }

    default void recordCompensateLatency(long durationMs) {
    }

    default void recordBatchMarkSucceeded() {
    }

    default void recordBatchMarkFailed() {
    }

    default void recordBatchMarkSize(int size) {
    }

    default void registerThreadPoolGauges(ThreadPoolExecutor pool) {
    }

}
