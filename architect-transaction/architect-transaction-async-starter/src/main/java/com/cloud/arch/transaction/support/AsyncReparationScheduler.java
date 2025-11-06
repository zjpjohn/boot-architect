package com.cloud.arch.transaction.support;

import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.AsyncTxExecutor;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import com.cloud.arch.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AsyncReparationScheduler implements SmartInitializingSingleton {

    public static final String ASYNC_REPARATION_MUTEX = "async-tx-reparation-mutex";

    private final AtomicInteger started = new AtomicInteger(0);

    private final MutexTemplate      mutexTemplate;
    private final IAsyncTxRepository repository;
    private final AsyncTxExecutor    asyncExecutor;

    public AsyncReparationScheduler(MutexTemplate mutexTemplate,
                                    IAsyncTxRepository repository,
                                    AsyncTxExecutor asyncExecutor) {
        this.mutexTemplate = mutexTemplate;
        this.repository    = repository;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 修复应异常关闭导致任务处于ready或运行中的异步任务
     */
    public void repairAsyncTask() {
        Duration           before = started.get() == 0 ? Duration.ofSeconds(30) : Duration.ofMinutes(5);
        List<AsyncTxEvent> events = repository.loadReadyRunning(before);
        if (CollectionUtils.isNotEmpty(events)) {
            events.forEach(this.asyncExecutor::execute);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        final ContendMutexProps mutexProps
                = new ContendMutexProps(Duration.ofSeconds(30), Duration.ofSeconds(30), Duration.ofSeconds(15));
        mutexTemplate.scheduleAtRate(mutexProps, ASYNC_REPARATION_MUTEX, mutexProps.getInitialDelay(), Duration.ofMinutes(5), this::repairAsyncTask);
    }

}
