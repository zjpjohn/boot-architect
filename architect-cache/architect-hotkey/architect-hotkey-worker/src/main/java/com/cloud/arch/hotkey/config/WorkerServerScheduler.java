package com.cloud.arch.hotkey.config;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WorkerServerScheduler {

    private final ScheduledExecutorService scheduler;

    public WorkerServerScheduler() {
        this.scheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("worker-scheduler"));
    }

    public void schedule(long initialDelay, long fixedRate, Runnable runnable) {
        scheduler.scheduleAtFixedRate(runnable,
                initialDelay,
                fixedRate,
                TimeUnit.SECONDS);
    }

    public void destroy() {
        this.scheduler.shutdownNow();
    }
}
