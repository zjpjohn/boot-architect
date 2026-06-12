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
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JdbcCompensateEventScheduler implements CompensateHandler, SmartInitializingSingleton {

    public static final String EVENT_COMPENSATE_MUTEX      = "compensate-event-mutex";
    public static final String DEAD_LETTER_MUTEX           = "dead-letter-mutex";
    public static final String CLEAN_SUCCEEDED_EVENT_MUTEX = "clean-event-mutex";

    private final JdbcCompensateProperties properties;
    private final MutexTemplate            mutexTemplate;
    private final IDomainEventRepository   eventRepository;
    private final JdbcCompensateProcessor  compensateProcessor;
    private       EventStatsManager        statsManager = EventStatsManager.disabled();

    public JdbcCompensateEventScheduler(MutexTemplate mutexTemplate, JdbcCompensateProperties properties, IDomainEventRepository eventRepository, JdbcCompensateProcessor compensateProcessor) {
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
     * 补偿发送处理器：补偿失败事件（version < maxVersion）。
     */
    @Override
    public void handle() {
        long metricStart = System.currentTimeMillis();
        statsManager.incrementCompensateCycle();

        final List<PublishEventEntity> entities = eventRepository.queryFailed(properties.getBatch(), properties.getMaxVersion(), properties.getBefore(), properties.getRange());
        if (!CollectionUtils.isEmpty(entities)) {
            entities.forEach(compensateProcessor::process);
            statsManager.incrementCompensateRetry(entities.size());
        }
        statsManager.recordCompensateLatency(System.currentTimeMillis() - metricStart);
    }

    /**
     * 死信归档：将超过最大重试次数的事件从事件表移入死信表。
     */
    public void archiveDeadLetters() {
        JdbcCompensateProperties.DeadLetter dl             = properties.getDeadLetter();
        Integer                             maxVersion     = properties.getMaxVersion();
        final List<PublishEventEntity>      deadCandidates = eventRepository.deadEventCandidates(dl.getBatch(), maxVersion, dl.getBefore(), dl.getRange());
        if (!CollectionUtils.isEmpty(deadCandidates)) {
            deadCandidates.forEach(entity -> eventRepository.archiveDeadEvent(entity, "exceeded max retry version " +
                                                                                      maxVersion));
            statsManager.incrementCompensateDeadLetter(deadCandidates.size());
        }
    }

    /**
     * 补偿发送任务
     */
    private void compensateSchedule() {
        final JdbcCompensateProperties.SchedulerMutex mutex      = this.properties.getMutex();
        final ContendMutexProps                       mutexProps = new ContendMutexProps(mutex.getInitialDelay(), mutex.getTtl(), mutex.getTransition());
        mutexTemplate.scheduleAtRate(mutexProps, EVENT_COMPENSATE_MUTEX, properties.getInitialDelay(), properties.getPeriod(), this::handle);
    }

    /**
     * 死信归档任务
     */
    private void deadLetterSchedule() {
        JdbcCompensateProperties.DeadLetter     dl        = this.properties.getDeadLetter();
        JdbcCompensateProperties.SchedulerMutex deadMutex = dl.getMutex();
        ContendMutexProps                       props     = new ContendMutexProps(deadMutex.getInitialDelay(), deadMutex.getTtl(), deadMutex.getTransition());
        mutexTemplate.scheduleAtRate(props, DEAD_LETTER_MUTEX, dl.getInitialDelay(), dl.getPeriod(), this::archiveDeadLetters);
    }

    /**
     * 清理超过保留期的成功事件（state=1），删除 7 天前已成功投递到 MQ 的事件。
     */
    public void cleanSucceededEvents() {
        int  days    = properties.getCleanSucceed().getRetainDays();
        int  limit   = properties.getCleanSucceed().getBatchSize();
        long before  = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        int  deleted = eventRepository.cleanSucceededEvents(before, limit);
        statsManager.incrementSucceededClean(deleted);
    }

    /**
     * 清理发送成功的事件任务
     */
    private void cleanEventSchedule() {
        JdbcCompensateProperties.CleanSucceed   cs         = properties.getCleanSucceed();
        JdbcCompensateProperties.SchedulerMutex cleanMutex = cs.getMutex();
        ContendMutexProps                       cleanProps = new ContendMutexProps(cleanMutex.getInitialDelay(), cleanMutex.getTtl(), cleanMutex.getTransition());
        mutexTemplate.scheduleAtRate(cleanProps, CLEAN_SUCCEEDED_EVENT_MUTEX, cs.getInitialDelay(), cs.getPeriod(), this::cleanSucceededEvents);
    }

    @Override
    public void afterSingletonsInstantiated() {
        //事件补偿定时任务
        this.compensateSchedule();
        //死信归档定时任务
        this.deadLetterSchedule();
        //成功事件定时清理任务
        this.cleanEventSchedule();
    }

}
