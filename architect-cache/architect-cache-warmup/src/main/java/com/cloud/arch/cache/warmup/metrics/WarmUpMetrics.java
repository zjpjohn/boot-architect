package com.cloud.arch.cache.warmup.metrics;

import com.cloud.arch.cache.warmup.core.WarmUpResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存预热 Micrometer 指标采集器
 */
@Slf4j
public class WarmUpMetrics {

    private static final String TOTAL_COUNTER_NAME  = "cache.warmup.total";
    private static final String DURATION_TIMER_NAME = "cache.warmup.duration";
    private static final String LOCK_ACQUIRED_NAME  = "cache.warmup.lock.acquired";
    private static final String LOCK_SKIPPED_NAME   = "cache.warmup.lock.skipped";

    private final MeterRegistry        registry;
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer>   durationTimers  = new ConcurrentHashMap<>();
    private final Map<String, Counter> lockAcquired    = new ConcurrentHashMap<>();
    private final Map<String, Counter> lockSkipped     = new ConcurrentHashMap<>();

    public WarmUpMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordLockAcquired(String cacheName) {
        if (registry == null) {
            return;
        }
        lockAcquired.computeIfAbsent(cacheName,
                                     c -> Counter.builder(LOCK_ACQUIRED_NAME)
                                                 .tags(Tags.of("cache", c))
                                                 .description("预热分布式锁获取成功次数")
                                                 .register(registry)).increment();
    }

    public void recordLockSkipped(String cacheName) {
        if (registry == null) {
            return;
        }
        lockSkipped.computeIfAbsent(cacheName,
                                    c -> Counter.builder(LOCK_SKIPPED_NAME)
                                                .tags(Tags.of("cache", c))
                                                .description("预热分布式锁跳过次数（其他节点正在执行）")
                                                .register(registry)).increment();
    }

    public void recordResult(WarmUpResult result) {
        String cacheName = result.getCacheName();
        if (cacheName == null || registry == null) {
            return;
        }

        Counter successCounter = successCounters.computeIfAbsent(cacheName,
                                                                 c -> Counter.builder(TOTAL_COUNTER_NAME)
                                                                             .tags(Tags.of("cache",
                                                                                           c,
                                                                                           "status",
                                                                                           "success"))
                                                                             .description("预热成功条目数")
                                                                             .register(registry));
        successCounter.increment(result.getSuccessCount());
        int failureCount = result.getTotalCount() - result.getSuccessCount();
        if (failureCount > 0) {
            Counter failureCounter = failureCounters.computeIfAbsent(cacheName,
                                                                     c -> Counter.builder(TOTAL_COUNTER_NAME)
                                                                                 .tags(Tags.of("cache",
                                                                                               c,
                                                                                               "status",
                                                                                               "failure"))
                                                                                 .description("预热失败条目数")
                                                                                 .register(registry));
            failureCounter.increment(failureCount);
        }

        Timer timer = durationTimers.computeIfAbsent(cacheName,
                                                     c -> Timer.builder(DURATION_TIMER_NAME)
                                                               .tags(Tags.of("cache", c))
                                                               .description("预热耗时")
                                                               .register(registry));
        timer.record(result.getDurationMs(), TimeUnit.MILLISECONDS);
    }

    public void report(List<WarmUpResult> results) {
        if (log.isInfoEnabled()) {
            results.stream().collect(Collectors.groupingBy(WarmUpResult::getCacheName)).forEach((cacheName, list) -> {
                int  totalCount    = list.stream().mapToInt(WarmUpResult::getTotalCount).sum();
                int  successCount  = list.stream().mapToInt(WarmUpResult::getSuccessCount).sum();
                long totalDuration = list.stream().mapToLong(WarmUpResult::getDurationMs).sum();
                log.info("[WarmUp] cache={} success={}/{} duration={}ms",
                         cacheName,
                         successCount,
                         totalCount,
                         totalDuration);
            });
            int  totalCount    = results.stream().mapToInt(WarmUpResult::getTotalCount).sum();
            int  successCount  = results.stream().mapToInt(WarmUpResult::getSuccessCount).sum();
            long totalDuration = results.stream().mapToLong(WarmUpResult::getDurationMs).sum();
            log.info("[WarmUp] ==== Total: {} tasks, {} total, {} success, {}ms ====",
                     results.size(),
                     totalCount,
                     successCount,
                     totalDuration);
        }
    }
}
