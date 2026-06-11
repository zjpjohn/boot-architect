package com.cloud.arch.cache.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缓存异步任务执行器，基于虚拟线程实现。
 * 所有提交的任务均为短时 Redis I/O 操作（TTL 刷新、缓存淘汰、预热编排），
 * 虚拟线程在阻塞 I/O 时自动 unmount 释放平台线程，无需配置线程数和队列。
 */
@Slf4j
public class CacheThreadPoolExecutor {

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(executor::close));
    }

    public static void run(Runnable runnable) {
        executor.execute(runnable);
    }

    public static <V> void submit(Callable<V> callable) {
        executor.submit(callable);
    }

}
