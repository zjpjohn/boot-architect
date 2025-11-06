package com.cloud.arch.hotkey.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncPool {
    private static final ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();

    public static void asyncDo(Runnable runnable) {
        threadPoolExecutor.submit(runnable);
    }

    public static void shutDown() {
        threadPoolExecutor.shutdown();
    }
}
