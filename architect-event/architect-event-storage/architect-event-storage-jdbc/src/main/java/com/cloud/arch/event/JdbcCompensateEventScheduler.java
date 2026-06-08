package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.CompensateHandler;
import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class JdbcCompensateEventScheduler implements CompensateHandler, SmartInitializingSingleton {

    public static final String EVENT_COMPENSATE_MUTEX = "compensate-event-mutex";

    private final JdbcCompensateProperties properties;
    private final MutexTemplate            mutexTemplate;
    private final IDomainEventRepository   eventRepository;
    private final JdbcCompensateProcessor  compensateProcessor;
    private       EventStatsManager        statsManager = EventStatsManager.disabled();

    public JdbcCompensateEventScheduler(MutexTemplate mutexTemplate,
                                        JdbcCompensateProperties properties,
                                        IDomainEventRepository eventRepository,
                                        JdbcCompensateProcessor compensateProcessor) {
        this.mutexTemplate = mutexTemplate;
        this.properties = properties;
        this.eventRepository = eventRepository;
        this.compensateProcessor = compensateProcessor;
    }

    public void setEventStatsManager(EventStatsManager statsManager) {
        if (statsManager != null) {
            this.statsManager = statsManager;
        }
    }

    /**
     * 补偿发送处理器：先补偿失败事件，再将超过最大重试次数的事件移入死信表。
     */
    @Override
    public void handle() {
        long metricStart = System.currentTimeMillis();
        statsManager.incrementCompensateCycle();

        final List<PublishEventEntity> entities = eventRepository.queryFailed(properties.getBatch(),
                                                                              properties.getMaxVersion(),
                                                                              properties.getBefore(),
                                                                              properties.getRange());
        if (!CollectionUtils.isEmpty(entities)) {
            entities.forEach(compensateProcessor::process);
            statsManager.incrementCompensateRetry(entities.size());
        }

        final List<PublishEventEntity> deadCandidates = eventRepository.deadEventCandidates(properties.getBatch(),
                                                                                            properties.getMaxVersion(),
                                                                                            properties.getBefore(),
                                                                                            properties.getRange());
        if (!CollectionUtils.isEmpty(deadCandidates)) {
            deadCandidates.forEach(entity -> eventRepository.archiveDeadEvent(entity,
                                                                              "exceeded max retry version " +
                                                                              properties.getMaxVersion()));
            statsManager.incrementCompensateDeadLetter(deadCandidates.size());
        }

        statsManager.recordCompensateLatency(System.currentTimeMillis() - metricStart);
    }

    @Override
    public void afterSingletonsInstantiated() {
        final JdbcCompensateProperties.SchedulerMutex mutex = this.properties.getMutex();
        final ContendMutexProps mutexProps = new ContendMutexProps(mutex.getInitialDelay(),
                                                                   mutex.getTtl(),
                                                                   mutex.getTransition());
        mutexTemplate.scheduleAtRate(mutexProps,
                                     EVENT_COMPENSATE_MUTEX,
                                     properties.getInitialDelay(),
                                     properties.getPeriod(),
                                     this::handle);
    }

}
