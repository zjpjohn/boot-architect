package com.cloud.arch.cache.warmup.core;

import com.cloud.arch.cache.warmup.metrics.WarmUpMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存预热执行器，基于虚拟线程并发执行所有 key 的预热
 */
@Slf4j
public class WarmUpExecutor {

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final WarmUpCoordinator coordinator;
    private final WarmUpMetrics     metrics;
    private final long              lockWaitSeconds;
    private final long              timeoutSeconds;
    private final ConversionService conversionService;

    public WarmUpExecutor(WarmUpCoordinator coordinator, WarmUpMetrics metrics, long lockWaitSeconds, long timeoutSeconds) {
        this.coordinator = coordinator;
        this.metrics = metrics;
        this.lockWaitSeconds = lockWaitSeconds;
        this.timeoutSeconds = timeoutSeconds;
        this.conversionService = DefaultConversionService.getSharedInstance();
    }

    /**
     * 执行单个缓存名下所有匹配方法的预热，锁在 Future 完成后释放
     */
    public CompletableFuture<WarmUpResult> execute(String cacheName, List<Object[]> args, List<WarmUpTask> tasks) {
        if (!coordinator.tryAcquireWarmUpLock(cacheName, lockWaitSeconds)) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] skip cache={}, another node is handling it", cacheName);
            }
            metrics.recordLockSkipped(cacheName);
            return CompletableFuture.completedFuture(null);
        }
        metrics.recordLockAcquired(cacheName);
        return doExecute(cacheName, args, tasks).whenComplete((r, ex) -> coordinator.releaseWarmUpLock(cacheName));
    }

    public CompletableFuture<List<WarmUpResult>> executeAll(String cacheName, List<Object[]> args, List<WarmUpTask> tasks) {
        return execute(cacheName, args, tasks).thenApply(result -> result ==
                                                                   null ? Collections.emptyList() : List.of(result));
    }

    private CompletableFuture<WarmUpResult> doExecute(String cacheName, List<Object[]> argsList, List<WarmUpTask> tasks) {
        if (argsList.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return invokeWithArgs(cacheName, argsList, tasks);
    }

    private CompletableFuture<WarmUpResult> invokeWithArgs(String cacheName, List<Object[]> argsList, List<WarmUpTask> tasks) {
        WarmUpResult result = new WarmUpResult();
        result.setCacheName(cacheName);
        result.setTotalCount(argsList.size());

        long          taskStart    = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);

        CompletableFuture<?>[] futures = argsList.stream().map(args -> CompletableFuture.runAsync(() -> {
            for (WarmUpTask task : tasks) {
                Method method = task.getMethod();
                if (method.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    Object[] convertedArgs = convertArgs(method, args);
                    method.invoke(task.getTargetBean(), convertedArgs);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    log.warn("[WarmUp] FAILED cache={} bean={} method={} error: {}", cacheName, task.getBeanName(), method.getName(), errorMsg);
                }
                break;
            }
        }, VIRTUAL_THREAD_EXECUTOR)).toArray(CompletableFuture[]::new);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        if (timeoutSeconds > 0) {
            allFutures = allFutures.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        }

        return allFutures.handle((v, ex) -> {
            result.setSuccessCount(successCount.get());
            result.setDurationMs(System.currentTimeMillis() - taskStart);
            result.setSuccess(true);
            return result;
        });
    }

    private Object[] convertArgs(Method method, Object[] rawArgs) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (rawArgs.length != paramTypes.length) {
            throw new IllegalArgumentException("Argument count mismatch: expected " +
                                               paramTypes.length +
                                               " but got " +
                                               rawArgs.length);
        }
        Object[] converted = new Object[rawArgs.length];
        for (int i = 0; i < rawArgs.length; i++) {
            if (rawArgs[i] == null || paramTypes[i].isInstance(rawArgs[i])) {
                converted[i] = rawArgs[i];
            } else {
                converted[i] = conversionService.convert(rawArgs[i], paramTypes[i]);
            }
        }
        return converted;
    }
}
