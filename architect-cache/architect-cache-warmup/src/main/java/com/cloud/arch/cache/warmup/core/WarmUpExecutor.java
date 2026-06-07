package com.cloud.arch.cache.warmup.core;

import com.cloud.arch.cache.utils.CacheThreadPoolExecutor;
import com.cloud.arch.cache.warmup.support.WarmUpMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存预热执行器，负责分布式锁协调下的反射调用与参数转换
 */
@Slf4j
public class WarmUpExecutor {

    private final WarmUpCoordinator coordinator;
    private final WarmUpMetrics     metrics;
    private final long              lockWaitSeconds;
    private final boolean           globalAsync;
    private final long              globalTimeout;
    private final ConversionService conversionService;

    public WarmUpExecutor(WarmUpCoordinator coordinator,
                          WarmUpMetrics metrics,
                          long lockWaitSeconds,
                          boolean globalAsync,
                          long globalTimeout) {
        this.coordinator = coordinator;
        this.metrics = metrics;
        this.lockWaitSeconds = lockWaitSeconds;
        this.globalAsync = globalAsync;
        this.globalTimeout = globalTimeout;
        this.conversionService = new DefaultConversionService();
    }

    /**
     * 执行单个缓存名下所有匹配方法的预热
     */
    public WarmUpResult execute(String cacheName, List<Object[]> args, List<WarmUpTask> tasks) {
        if (!coordinator.tryAcquireWarmUpLock(cacheName, lockWaitSeconds)) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] skip cache={}, another node is handling it", cacheName);
            }
            metrics.recordLockSkipped(cacheName);
            return null;
        }
        metrics.recordLockAcquired(cacheName);
        try {
            return doExecute(cacheName, args, tasks);
        } finally {
            coordinator.releaseWarmUpLock(cacheName);
        }
    }

    public List<WarmUpResult> executeAll(String cacheName, List<Object[]> args, List<WarmUpTask> tasks) {
        List<WarmUpResult> results = new ArrayList<>();
        WarmUpResult       result  = execute(cacheName, args, tasks);
        if (result != null) {
            results.add(result);
        }
        return results;
    }

    private WarmUpResult doExecute(String cacheName, List<Object[]> argsList, List<WarmUpTask> tasks) {
        if (argsList.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] no args configured for cache={}, skipping", cacheName);
            }
            return null;
        }

        if (globalAsync) {
            final String           fCacheName = cacheName;
            final List<Object[]>   fArgsList  = argsList;
            final List<WarmUpTask> fTasks     = tasks;
            CacheThreadPoolExecutor.run(() -> {
                WarmUpResult asyncResult = invokeWithArgs(fCacheName, fArgsList, fTasks, globalTimeout);
                metrics.recordResult(asyncResult);
            });
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] submitted async warm-up for cache={}, {} args", cacheName, argsList.size());
            }
            return null;
        }

        WarmUpResult result = invokeWithArgs(cacheName, argsList, tasks, globalTimeout);
        metrics.recordResult(result);
        return result;
    }

    private WarmUpResult invokeWithArgs(String cacheName,
                                        List<Object[]> argsList,
                                        List<WarmUpTask> tasks,
                                        long timeoutSeconds) {
        WarmUpResult result = new WarmUpResult();
        result.setCacheName(cacheName);
        result.setTotalCount(argsList.size());

        long taskStart    = System.currentTimeMillis();
        int  successCount = 0;

        for (int i = 0; i < argsList.size(); i++) {
            Object[] args = argsList.get(i);

            for (WarmUpTask task : tasks) {
                Method method = task.getMethod();
                if (method.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    Object[] convertedArgs = convertArgs(method, args);
                    method.invoke(task.getTargetBean(), convertedArgs);
                    successCount++;
                } catch (Exception e) {
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    log.warn("[WarmUp] FAILED cache={} bean={} method={} args[{}] error: {}",
                             cacheName,
                             task.getBeanName(),
                             method.getName(),
                             i,
                             errorMsg);
                }
                break; // 匹配到第一个参数个数一致的方法就停止
            }

            if (timeoutSeconds > 0 && (System.currentTimeMillis() - taskStart) > timeoutSeconds * 1000) {
                log.warn("[WarmUp] timeout for cache={}, processed {}/{}, stopping", cacheName, i + 1, argsList.size());
                break;
            }
        }

        result.setSuccessCount(successCount);
        result.setDurationMs(System.currentTimeMillis() - taskStart);
        result.setSuccess(true);
        return result;
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
