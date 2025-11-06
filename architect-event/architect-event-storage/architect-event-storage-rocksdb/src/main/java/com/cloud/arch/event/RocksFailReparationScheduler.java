package com.cloud.arch.event;

import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RocksFailReparationScheduler implements DisposableBean, SmartInitializingSingleton {

    private final IDomainEventRepository    eventRepository;
    private final RocksReparationProcessor  reparationProcessor;
    private final RocksCompensateProperties properties;
    private final ScheduledExecutorService  scheduledExecutor;

    public RocksFailReparationScheduler(IDomainEventRepository eventRepository,
                                        RocksReparationProcessor reparationProcessor,
                                        RocksCompensateProperties properties) {
        this.eventRepository     = eventRepository;
        this.reparationProcessor = reparationProcessor;
        this.properties          = properties;
        this.scheduledExecutor   = new ScheduledThreadPoolExecutor(2,
                                                                   Threads.threadFactory("event-reparation-scheduler"),
                                                                   new ThreadPoolExecutor.DiscardPolicy());
    }

    protected void reparationHandle() {
        final List<PublishEventEntity> entities = eventRepository.queryFailed(properties.getBatch(),
                                                                              0,
                                                                              properties.getBefore(),
                                                                              null);
        if (!CollectionUtils.isEmpty(entities)) {
            entities.forEach(reparationProcessor::push);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.scheduledExecutor.scheduleAtFixedRate(this::reparationHandle,
                                                   properties.getInitialDelay().getSeconds(),
                                                   properties.getPeriod().getSeconds(),
                                                   TimeUnit.SECONDS);
    }

    @Override
    public void destroy() throws Exception {
        this.scheduledExecutor.shutdownNow();
    }

}
