package com.cloud.arch.cache.warmup.core;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 手动预热 API，支持按缓存名触发预热，参数可从配置或调用方传入
 */
@Slf4j
public class WarmUpTemplate {

    private final WarmUpRegistry     registry;
    private final WarmUpExecutor     executor;
    private final WarmUpArgsProvider argsProvider;

    public WarmUpTemplate(WarmUpRegistry registry, WarmUpExecutor executor, WarmUpArgsProvider argsProvider) {
        this.registry = registry;
        this.executor = executor;
        this.argsProvider = argsProvider;
    }

    /**
     * 按缓存名预热，参数从 YAML 配置读取
     */
    public WarmUpResult warmUp(String cacheName) {
        List<Object[]>   args  = argsProvider.provide(cacheName);
        List<WarmUpTask> tasks = registry.getTasksByCache(cacheName);
        if (tasks.isEmpty()) {
            log.warn("[WarmUp] no registered methods for cache={}", cacheName);
            return null;
        }
        return executor.execute(cacheName, args, tasks);
    }

    /**
     * 按缓存名预热，参数由调用方提供
     */
    public WarmUpResult warmUp(String cacheName, List<Object[]> args) {
        List<WarmUpTask> tasks = registry.getTasksByCache(cacheName);
        if (tasks.isEmpty()) {
            log.warn("[WarmUp] no registered methods for cache={}", cacheName);
            return null;
        }
        return executor.execute(cacheName, args, tasks);
    }
}
