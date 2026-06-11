package com.cloud.arch.cache.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheThreadPoolExecutor {

    private static final long KEEP_ALIVE_TIME   = 120;
    private static final int  WORKER_QUEUE_SIZE = 256;

    private static volatile ThreadPoolExecutor taskExecutor = null;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (taskExecutor != null) {
                taskExecutor.shutdownNow();
            }
        }));
    }

    private static ThreadPoolExecutor executor() {
        if (taskExecutor != null) {
            return taskExecutor;
        }
        synchronized (CacheThreadPoolExecutor.class) {
            if (taskExecutor == null) {
                int processors = Runtime.getRuntime().availableProcessors();
                BasicThreadFactory threadFactory = BasicThreadFactory.builder()
                                                                     .namingPattern("cache-task-pool-")
                                                                     .build();
                taskExecutor = new ThreadPoolExecutor(processors,
                                                      processors * 2 + 1,
                                                      KEEP_ALIVE_TIME,
                                                      TimeUnit.SECONDS,
                                                      new LinkedBlockingQueue<>(WORKER_QUEUE_SIZE),
                                                      threadFactory,
                                                      new TaskRejectedPolicy());
            }
        }
        return taskExecutor;
    }

    public static void run(Runnable runnable) {
        executor().execute(runnable);
    }

    public static <V> void submit(Callable<V> callable) {
        executor().submit(callable);
    }

    private static class TaskRejectedPolicy extends ThreadPoolExecutor.DiscardOldestPolicy {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.warn("缓存任务线程池队列已满(pool:{}/active:{}/queue:{})，丢弃最旧任务",
                     e.getPoolSize(),
                     e.getActiveCount(),
                     e.getQueue().size());
            super.rejectedExecution(r, e);
        }
    }

}
