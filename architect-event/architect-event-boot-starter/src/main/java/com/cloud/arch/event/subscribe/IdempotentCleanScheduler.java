package com.cloud.arch.event.subscribe;

import com.cloud.arch.event.props.PublishEventProperties;
import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class IdempotentCleanScheduler implements SmartInitializingSingleton {

    public static final String IDEMPOTENT_CLEAN_MUTEX = "idempotent_clean_mutex";

    private final PublishEventProperties properties;
    private final MutexTemplate          mutexTemplate;
    private final IdempotentChecker      idempotentChecker;

    public IdempotentCleanScheduler(PublishEventProperties properties,
                                    MutexTemplate mutexTemplate,
                                    IdempotentChecker idempotentChecker) {
        this.properties        = properties;
        this.mutexTemplate     = mutexTemplate;
        this.idempotentChecker = idempotentChecker;
    }

    protected void cleanWork() {
        Duration      before   = properties.getSubscriber().getBefore();
        LocalDateTime dateTime = LocalDateTime.now().minus(before.toMillis(), ChronoUnit.MILLIS);
        idempotentChecker.garbageClean(dateTime);
    }

    @Override
    public void afterSingletonsInstantiated() {
        final PublishEventProperties.Subscriber     subscriber = properties.getSubscriber();
        final PublishEventProperties.SchedulerMutex mutex      = subscriber.getMutex();
        final ContendMutexProps props
                = new ContendMutexProps(mutex.getInitialDelay(), mutex.getTtl(), mutex.getTransition());
        mutexTemplate.scheduleAtRate(props, IDEMPOTENT_CLEAN_MUTEX, subscriber.getInitialDelay(), subscriber.getPeriod(), this::cleanWork);
    }

}
