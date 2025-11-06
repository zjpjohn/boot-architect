package com.cloud.arch.mutex.schedule;

import com.cloud.arch.mutex.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MutexScheduler extends AbsMutexContender {

    private final    Runnable                 workTask;
    private final    SchedulerConfig          config;
    private final    ContendController        controller;
    private final    ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?>       workFuture;

    public MutexScheduler(String mutex,
                          SchedulerConfig config,
                          Runnable workTask,
                          ContendMutexProps props,
                          ScheduledExecutorService scheduler,
                          ContendControllerFactory controllerFactory) {
        super(mutex);
        this.config     = config;
        this.workTask   = workTask;
        this.scheduler  = scheduler;
        this.controller = controllerFactory.createContendController(this, props);
    }

    public MutexScheduler(String mutex,
                          SchedulerConfig config,
                          Runnable workTask,
                          ContendMutexProps props,
                          ContendControllerFactory controllerFactory) {
        this(mutex, config, workTask, props, Executors.newSingleThreadScheduledExecutor(), controllerFactory);
    }

    /**
     * 获取到竞争资源回调
     */
    @Override
    public void onAcquired(MutexState state) {
        if (this.workFuture != null && !this.workFuture.isCancelled() && !this.workFuture.isDone()) {
            return;
        }
        final long initialDelay = config.getInitialDelay().toMillis();
        final long period       = config.getPeriod().toMillis();
        if (config.getType() == ScheduleType.FIXED_RATE) {
            this.workFuture
                    = this.scheduler.scheduleAtFixedRate(this::safeWork, initialDelay, period, TimeUnit.MILLISECONDS);
            return;
        }
        this.workFuture
                = this.scheduler.scheduleWithFixedDelay(this::safeWork, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 释放竞争资源回调
     */
    @Override
    public void onRelease(MutexState state) {
        if (this.workFuture != null) {
            this.workFuture.cancel(true);
        }
    }

    private void safeWork() {
        try {
            this.workTask.run();
        } catch (Exception error) {
            log.error(error.getMessage(), error);
        }
    }

    public void start() {
        //竞争者者预初始准备资源
        controller.prepareMutex();
        //开始竞争资源
        controller.start();
    }

    public void stop() {
        //停止释放资源
        controller.stop();
    }

}
