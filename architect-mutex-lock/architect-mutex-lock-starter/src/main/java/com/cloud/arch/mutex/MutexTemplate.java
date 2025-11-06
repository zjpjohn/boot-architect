package com.cloud.arch.mutex;

import com.cloud.arch.mutex.core.ContendControllerFactory;
import com.cloud.arch.mutex.core.ContendMutexProps;
import com.cloud.arch.mutex.lock.Lock;
import com.cloud.arch.mutex.lock.MutexLock;
import com.cloud.arch.mutex.schedule.MutexScheduler;
import com.cloud.arch.mutex.schedule.SchedulerConfig;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class MutexTemplate implements SmartLifecycle {

    private final    Map<String, MutexScheduler> schedulers = Maps.newConcurrentMap();
    private volatile boolean                     running    = false;

    private final ScheduledExecutorService scheduledExecutor;
    private final ContendControllerFactory controllerFactory;

    public MutexTemplate(ScheduledExecutorService scheduledExecutor, ContendControllerFactory controllerFactory) {
        this.scheduledExecutor = scheduledExecutor;
        this.controllerFactory = controllerFactory;
    }

    public Lock mutexLock(String mutex) {
        return mutexLock(mutex, new ContendMutexProps());
    }

    public Lock mutexLock(String mutex, ContendMutexProps props) {
        return new MutexLock(mutex, props, controllerFactory);
    }

    public void scheduleAtRate(ContendMutexProps props,
                               String mutex,
                               Duration initDelay,
                               Duration period,
                               Runnable task) {
        final SchedulerConfig config = SchedulerConfig.ofRate(initDelay, period);
        this.schedule(mutex, props, config, task);
    }

    public void scheduleAtRate(String mutex, Duration initDelay, Duration period, Runnable task) {
        final SchedulerConfig config = SchedulerConfig.ofRate(initDelay, period);
        this.schedule(mutex, new ContendMutexProps(), config, task);
    }

    public void scheduleAtDelay(ContendMutexProps props,
                                String mutex,
                                Duration initDelay,
                                Duration delay,
                                Runnable task) {
        final SchedulerConfig config = SchedulerConfig.ofDelay(initDelay, delay);
        this.schedule(mutex, props, config, task);
    }

    public void scheduleAtDelay(String mutex, Duration initDelay, Duration delay, Runnable task) {
        final SchedulerConfig config = SchedulerConfig.ofDelay(initDelay, delay);
        this.schedule(mutex, new ContendMutexProps(), config, task);
    }

    private void schedule(String mutex, ContendMutexProps props, SchedulerConfig config, Runnable task) {
        Preconditions.checkState(StringUtils.isNotBlank(mutex), "mutex must not be null.");
        if (schedulers.containsKey(mutex)) {
            throw new IllegalArgumentException(Strings.lenientFormat("exist mutex [%s] scheduler, please change another mutex.", mutex));
        }
        ScheduledExecutorService scheduleService = scheduledExecutor;
        if (scheduleService == null) {
            scheduleService = Executors.newSingleThreadScheduledExecutor();
        }
        final MutexScheduler mutexScheduler
                = new MutexScheduler(mutex, config, task, props, scheduleService, controllerFactory);
        schedulers.put(mutex, mutexScheduler);
        mutexScheduler.start();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void start() {
        if (this.running) {
            throw new IllegalStateException("mutex template is already running.");
        }
        this.running = true;
    }

    @Override
    public void stop() {
        if (this.running) {
            schedulers.values().forEach(MutexScheduler::stop);
            this.running = false;
        }
    }

    @Override
    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
