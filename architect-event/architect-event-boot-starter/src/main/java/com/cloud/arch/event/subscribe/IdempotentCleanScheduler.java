package com.cloud.arch.event.subscribe;

import com.cloud.arch.event.props.EventProperties;
import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 幂等记录清理调度器，利用分布式互斥锁定时清理过期的幂等记录，避免多节点重复执行。
 */
public class IdempotentCleanScheduler implements SmartInitializingSingleton {

    public static final String IDEMPOTENT_CLEAN_MUTEX = "idempotent_clean_mutex";

    private final EventProperties   properties;
    private final MutexTemplate     mutexTemplate;
    private final IdempotentChecker idempotentChecker;

    public IdempotentCleanScheduler(EventProperties properties, MutexTemplate mutexTemplate, IdempotentChecker idempotentChecker) {
        this.properties = properties;
        this.mutexTemplate = mutexTemplate;
        this.idempotentChecker = idempotentChecker;
    }

    protected void cleanWork() {
        Duration      before   = properties.getSubscriber().getBefore();
        LocalDateTime dateTime = LocalDateTime.now().minus(before.toMillis(), ChronoUnit.MILLIS);
        idempotentChecker.garbageClean(dateTime);
    }

    @Override
    public void afterSingletonsInstantiated() {
        final EventProperties.Subscriber     subscriber = properties.getSubscriber();
        final EventProperties.SchedulerMutex mutex      = subscriber.getMutex();
        final ContendMutexProps              props      = new ContendMutexProps(mutex.getInitialDelay(), mutex.getTtl(), mutex.getTransition());
        mutexTemplate.scheduleAtRate(props, IDEMPOTENT_CLEAN_MUTEX, subscriber.getInitialDelay(), subscriber.getPeriod(), this::cleanWork);
    }

}
