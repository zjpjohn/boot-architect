package com.cloud.arch.transaction.support;

import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import com.cloud.arch.transaction.config.AsyncTaskProperties;
import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class AsyncCompensateScheduler implements SmartInitializingSingleton {

    public static final String ASYNC_COMPENSATE_MUTEX = "async-tx-compensate-mutex";

    private final MutexTemplate       mutexTemplate;
    private final IAsyncTxRepository  repository;
    private final AsyncRetryQueue     retryQueue;
    private final AsyncTaskProperties properties;

    public void compensate() {
        List<AsyncTxEvent> txEvents = repository.queryFailed(properties.getBatch(), properties.getPeriod());
        retryQueue.delay(txEvents);
    }

    @Override
    public void afterSingletonsInstantiated() {
        final AsyncTaskProperties.SchedulerMutex mutex = this.properties.getMutex();
        final ContendMutexProps mutexProps = new ContendMutexProps(mutex.getInitialDelay(),
                                                                   mutex.getTtl(),
                                                                   mutex.getTransition());
        mutexTemplate.scheduleAtRate(mutexProps,
                                     ASYNC_COMPENSATE_MUTEX,
                                     properties.getInitialDelay(),
                                     properties.getPeriod(),
                                     this::compensate);
    }

}
