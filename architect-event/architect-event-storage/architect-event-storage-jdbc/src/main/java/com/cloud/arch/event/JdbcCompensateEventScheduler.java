package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.CompensateHandler;
import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JdbcCompensateEventScheduler implements CompensateHandler, SmartInitializingSingleton {

    public static final String EVENT_COMPENSATE_MUTEX          = "compensate-event-mutex";
    public static final String CLEAN_SUCCEEDED_EVENT_MUTEX     = "clean-succeeded-event-mutex";

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

    /**
     * 清理超过保留期的成功事件（state=1），删除 7 天前已成功投递到 MQ 的事件。
     */
    public void cleanSucceededEvents() {
        int days    = properties.getCleanSucceeded().getRetainDays();
        int limit   = properties.getCleanSucceeded().getBatchSize();
        long before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        int deleted = eventRepository.cleanSucceededEvents(before, limit);
        if (log.isInfoEnabled()) {
            log.info("Cleaned {} succeeded events older than {} days", deleted, days);
        }
        statsManager.incrementSucceededClean(deleted);
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

        // 成功事件清理：首次执行在下一个凌晨 3:00，之后每 24 小时
        JdbcCompensateProperties.CleanSucceeded cs         = properties.getCleanSucceeded();
        JdbcCompensateProperties.SchedulerMutex cleanMutex = cs.getMutex();
        ContendMutexProps                       cleanProps = new ContendMutexProps(cleanMutex.getInitialDelay(),
                                                                                    cleanMutex.getTtl(),
                                                                                    cleanMutex.getTransition());
        mutexTemplate.scheduleAtRate(cleanProps,
                                     CLEAN_SUCCEEDED_EVENT_MUTEX,
                                     initialDelayForNextTime(3, 0),
                                     Duration.ofDays(1),
                                     this::cleanSucceededEvents);
    }

    private Duration initialDelayForNextTime(int hour, int minute) {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime target = now.toLocalDate().atTime(hour, minute);
        if (!now.isBefore(target)) {
            target = target.plusDays(1);
        }
        return Duration.ofMillis(Duration.between(now, target).toMillis());
    }

}
