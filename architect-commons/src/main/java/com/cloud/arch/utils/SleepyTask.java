package com.cloud.arch.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class SleepyTask implements Runnable {

    private final   AtomicBoolean   ready   = new AtomicBoolean();
    private final   AtomicBoolean   running = new AtomicBoolean(false);
    protected final ExecutorService executor;

    public SleepyTask(ExecutorService executor) {
        this.executor = executor;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        ready.set(false);
    }

    @Override
    public void run() {
        try {
            while (ready.compareAndSet(true, false)) {
                try {
                    runTask();
                } catch (RuntimeException error) {
                    log.error(error.getMessage(), error);
                }
            }
        } finally {
            running.set(false);
        }
    }

    public void wakeup() {
        ready.set(true);
        if (running.compareAndSet(false, true)) {
            executor.execute(this);
        }
    }

    protected abstract void runTask();

}
