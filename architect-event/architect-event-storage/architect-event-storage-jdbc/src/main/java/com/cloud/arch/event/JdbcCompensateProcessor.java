package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.BatchEventMarker;
import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.metrics.EventStatsCounter;
import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class JdbcCompensateProcessor implements ApplicationContextAware, SmartInitializingSingleton {

    private final BatchEventMarker       batchMarker;
    private final IDomainEventRepository eventRepository;

    private EventPublisher     eventPublisher;
    private ApplicationContext applicationContext;
    private EventStatsManager  statsManager = EventStatsManager.disabled();

    public JdbcCompensateProcessor(IDomainEventRepository eventRepository, BatchEventMarker batchMarker) {
        this.batchMarker = batchMarker;
        this.eventRepository = eventRepository;
    }

    public void setStatsManager(EventStatsManager statsManager) {
        if (statsManager != null) {
            this.statsManager = statsManager;
        }
    }

    /**
     * 批量补偿：按 topic 分组后调用 {@link EventPublisher#publishBatch}，收集审计记录并批量写入。
     */
    public void processBatch(List<PublishEventEntity> entities) {
        Map<String, List<PublishEventEntity>> grouped = CollectionUtils.groupBy(entities, PublishEventEntity::getName);
        grouped.forEach((topic, events) -> {
            events.forEach(this::calcDelay);
            List<EventMessage>            publishMessages = events.stream().map(PublishEventEntity::build).toList();
            List<CompletableFuture<Void>> publishFutures  = eventPublisher.publishBatch(publishMessages);

            EventCompensateEntity[]       audits     = new EventCompensateEntity[events.size()];
            long                          batchStart = System.currentTimeMillis();
            List<CompletableFuture<Void>> chained    = new ArrayList<>();
            for (int i = 0; i < publishFutures.size(); i++) {
                final int          index  = i;
                PublishEventEntity entity = events.get(i);
                CompletableFuture<Void> future = publishFutures.get(i).whenComplete((v, ex) -> {
                    long taken = System.currentTimeMillis() - batchStart;
                    audits[index] = buildAudit(entity, batchStart, taken, ex);
                    EventStatsCounter statsCounter = statsManager.statsCounter(entity.getName());
                    if (ex != null) {
                        statsCounter.recordPublishFailure(taken);
                        batchMarker.markFailed(entity);
                    } else {
                        statsCounter.recordPublishSuccess(taken);
                        batchMarker.markSucceeded(entity);
                    }
                });
                chained.add(future);
            }
            CompletableFuture.allOf(chained.toArray(CompletableFuture[]::new))
                             .whenComplete((v, ex) -> eventRepository.batchCompensate(Arrays.asList(audits)));
        });
    }

    private EventCompensateEntity buildAudit(PublishEventEntity entity, long start, long taken, Throwable error) {
        EventCompensateEntity compensate = new EventCompensateEntity();
        compensate.setId(IdWorker.nextId());
        compensate.setEventId(entity.getId());
        compensate.setShardingKey(entity.getShardingKey());
        compensate.setStartTime(start);
        compensate.setTaken(taken);
        compensate.setGmtCreate(LocalDateTime.now());
        if (error != null) {
            compensate.setFailedMsg(extractMessage(error));
        }
        return compensate;
    }

    private static String extractMessage(Throwable error) {
        if (error.getMessage() != null) {
            return error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        return error.getClass().getSimpleName();
    }

    /**
     * 计算补偿事件延迟消息延迟时间
     */
    private void calcDelay(PublishEventEntity entity) {
        Long delay = entity.getDelay();
        if (delay != null && delay > 0) {
            Long gmtCreate = entity.getGmtCreate();
            long delta     = System.currentTimeMillis() - gmtCreate;
            entity.setDelay(delay <= delta ? 0 : delay - delta);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.eventPublisher = this.applicationContext.getBean(EventPublisher.class);
        Assert.notNull(this.eventPublisher, "EventPublisher bean not found");
    }

}
