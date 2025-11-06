package com.cloud.arch.cache.utils;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CacheThreadPoolExecutor {

    private static final long THREAD_KEEP_ALIVE_TIME   = 120;
    private static final int  THREAD_WORKER_QUEUE_SIZE = 256;

    private static ThreadPoolExecutor taskExecutor = null;

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
                taskExecutor = new ThreadPoolExecutor(processors,
                        processors * 2 + 1,
                        THREAD_KEEP_ALIVE_TIME,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(THREAD_WORKER_QUEUE_SIZE),
                        new BasicThreadFactory.Builder().namingPattern("cache-task-pool-").build(),
                        new ThreadPoolExecutor.DiscardOldestPolicy());
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

}
