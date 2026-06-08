package com.cloud.arch.event.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Micrometer 的事件指标管理器，参考 {@code architect-cache} 的 {@code CacheStatsManager}。
 * <p>
 * 为每个 topic 创建独立的 {@link MicroMeterStatsCounter}（topic 作为固定 tag 在构造时内嵌），
 * 全局指标（补偿、批量标记）在构造时 eager 注册。
 */
public class MicroMeterStatsManager implements EventStatsManager {

    private static final String COMPENSATE_CYCLE      = "domain.event.compensate.cycle";
    private static final String COMPENSATE_EVENTS     = "domain.event.compensate.events";
    private static final String COMPENSATE_LATENCY    = "domain.event.compensate.latency";
    private static final String THREADPOOL_QUEUE_SIZE = "domain.event.publisher.threadpool.queue.size";
    private static final String THREADPOOL_ACTIVE     = "domain.event.publisher.threadpool.active.threads";
    private static final String BATCH_MARK            = "domain.event.batch.mark";
    private static final String BATCH_MARK_SIZE       = "domain.event.batch.mark.size";

    private final MeterRegistry                  registry;
    private final Map<String, EventStatsCounter> counters;

    private final Counter             compensateCycleCounter;
    private final Counter             compensateRetryCount;
    private final Counter             compensateDeadLetterCount;
    private final Timer               compensateTimer;
    private final Counter             batchMarkSucceededCount;
    private final Counter             batchMarkFailedCount;
    private final DistributionSummary batchMarkSizeSummary;

    public MicroMeterStatsManager(MeterRegistry registry) {
        this.registry = registry;
        this.counters = new ConcurrentHashMap<>();

        this.compensateCycleCounter = Counter.builder(COMPENSATE_CYCLE)
                                             .description("事件补偿周期执行次数")
                                             .register(registry);
        this.compensateRetryCount = Counter.builder(COMPENSATE_EVENTS)
                                           .tags("type", "retry")
                                           .description("补偿事件处理条数")
                                           .register(registry);
        this.compensateDeadLetterCount = Counter.builder(COMPENSATE_EVENTS)
                                                .tags("type", "dead_letter")
                                                .description("补偿事件处理条数")
                                                .register(registry);
        this.compensateTimer = Timer.builder(COMPENSATE_LATENCY).description("补偿周期执行耗时").register(registry);
        this.batchMarkSucceededCount = Counter.builder(BATCH_MARK)
                                              .tags("status", "succeeded")
                                              .description("事件批量标记次数")
                                              .register(registry);
        this.batchMarkFailedCount = Counter.builder(BATCH_MARK)
                                           .tags("status", "failed")
                                           .description("事件批量标记次数")
                                           .register(registry);
        this.batchMarkSizeSummary = DistributionSummary.builder(BATCH_MARK_SIZE)
                                                       .description("每批次标记条目数")
                                                       .register(registry);
    }

    @Override
    public EventStatsCounter statsCounter(String topic) {
        return counters.computeIfAbsent(topic, k -> new MicroMeterStatsCounter(registry, k));
    }

    @Override
    public void incrementCompensateCycle() {
        compensateCycleCounter.increment();
    }

    @Override
    public void incrementCompensateRetry(int count) {
        compensateRetryCount.increment(count);
    }

    @Override
    public void incrementCompensateDeadLetter(int count) {
        compensateDeadLetterCount.increment(count);
    }

    @Override
    public void recordCompensateLatency(long durationMs) {
        compensateTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordBatchMarkSucceeded() {
        batchMarkSucceededCount.increment();
    }

    @Override
    public void recordBatchMarkFailed() {
        batchMarkFailedCount.increment();
    }

    @Override
    public void recordBatchMarkSize(int size) {
        batchMarkSizeSummary.record(size);
    }

    @Override
    public void registerThreadPoolGauges(ThreadPoolExecutor pool) {
        Gauge.builder(THREADPOOL_QUEUE_SIZE, pool, p -> p.getQueue().size())
             .description("发布线程池队列大小")
             .register(registry);
        Gauge.builder(THREADPOOL_ACTIVE, pool, ThreadPoolExecutor::getActiveCount)
             .description("发布线程池活跃线程数")
             .register(registry);
    }

}
