package com.cloud.arch.event.metrics;

/**
 * 单个队列的运营指标采集器，参考 {@code architect-cache} 的 {@code StatsCounter} 模式。
 * <p>
 * 每个实例绑定一个固定的 topic tag（构造时内嵌），所有方法无需再传入队列类型。
 * 实例由 {@link EventStatsManager#statsCounter(String)} 创建。
 */
public interface EventStatsCounter {

    static EventStatsCounter disabled() {
        return DisabledStatsCounter.INSTANCE;
    }

    void recordPublishSuccess(long durationMs);

    void recordPublishFailure(long durationMs);

    void recordPublishFallbackSync();

    void recordConsumeSuccess(long durationMs);

    void recordConsumeFailure(long durationMs);

    void recordConsumeDuplicate(long durationMs);

}
