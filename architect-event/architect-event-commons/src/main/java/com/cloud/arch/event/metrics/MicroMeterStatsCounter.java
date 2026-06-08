package com.cloud.arch.event.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 单个 topic 的 Micrometer 指标采集器，参考 {@code architect-cache} 的 {@code MicroMeterStatsCounter}。
 * <p>
 * 所有 Meter 在构造时 eager 注册，topic tag 固定内嵌，无需运行时拼 tag。
 */
public class MicroMeterStatsCounter implements EventStatsCounter {

    private static final String PUBLISH_COUNTER       = "domain.event.publish";
    private static final String PUBLISH_LATENCY       = "domain.event.publish.latency";
    private static final String PUBLISH_FALLBACK_SYNC = "domain.event.publish.fallback.sync";
    private static final String CONSUME_COUNTER       = "domain.event.consume";
    private static final String CONSUME_LATENCY       = "domain.event.consume.latency";

    private final Counter publishSuccessCount;
    private final Counter publishFailCount;
    private final Timer   publishTimer;
    private final Counter fallbackSyncCount;
    private final Counter consumeSuccessCount;
    private final Counter consumeFailCount;
    private final Counter consumeDuplicateCount;
    private final Timer   consumeTimer;

    public MicroMeterStatsCounter(MeterRegistry registry, String name) {
        Tags tags = Tags.of("queue", name);

        this.publishSuccessCount = Counter.builder(PUBLISH_COUNTER)
                .tags(tags).tag("status", "success")
                .description("领域事件发布次数")
                .register(registry);
        this.publishFailCount = Counter.builder(PUBLISH_COUNTER)
                .tags(tags).tag("status", "failure")
                .description("领域事件发布次数")
                .register(registry);
        this.publishTimer = Timer.builder(PUBLISH_LATENCY)
                .tags(tags)
                .description("领域事件发布耗时")
                .register(registry);
        this.fallbackSyncCount = Counter.builder(PUBLISH_FALLBACK_SYNC)
                .tags(tags)
                .description("发布线程池满时降级为同步发送次数")
                .register(registry);

        this.consumeSuccessCount = Counter.builder(CONSUME_COUNTER)
                .tags(tags).tag("status", "success")
                .description("领域事件消费次数")
                .register(registry);
        this.consumeFailCount = Counter.builder(CONSUME_COUNTER)
                .tags(tags).tag("status", "failure")
                .description("领域事件消费次数")
                .register(registry);
        this.consumeDuplicateCount = Counter.builder(CONSUME_COUNTER)
                .tags(tags).tag("status", "duplicate")
                .description("领域事件消费次数")
                .register(registry);
        this.consumeTimer = Timer.builder(CONSUME_LATENCY)
                .tags(tags)
                .description("领域事件消费耗时")
                .register(registry);
    }

    @Override
    public void recordPublishSuccess(long durationMs) {
        publishSuccessCount.increment();
        publishTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordPublishFailure(long durationMs) {
        publishFailCount.increment();
        publishTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordPublishFallbackSync() {
        fallbackSyncCount.increment();
    }

    @Override
    public void recordConsumeSuccess(long durationMs) {
        consumeSuccessCount.increment();
        consumeTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConsumeFailure(long durationMs) {
        consumeFailCount.increment();
        consumeTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConsumeDuplicate(long durationMs) {
        consumeDuplicateCount.increment();
        consumeTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

}
