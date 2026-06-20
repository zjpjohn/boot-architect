package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MessageQueuePublisher implements ApplicationContextAware, SmartInitializingSingleton {

    private final Semaphore              semaphore;
    private final BatchEventMarker       batchMarker;
    private       EventPublisher         eventPublisher;
    private       IDomainEventRepository eventRepository;
    private       EventMetadataFactory   eventMetadataFactory;
    private       EventStatsManager      statsManager = EventStatsManager.disabled();
    private       ApplicationContext     applicationContext;

    public MessageQueuePublisher(Integer maxConcurrency, BatchEventMarker batchMarker) {
        this.semaphore = new Semaphore(maxConcurrency);
        this.batchMarker = batchMarker;
    }

    public void setEventStatsManager(EventStatsManager statsManager) {
        if (statsManager != null) {
            this.statsManager = statsManager;
        }
    }


    /**
     * 直接异步发送非事务事件消息
     *
     * @param event 事件内容
     */
    public List<CompletableFuture<Void>> publish(Object event) {
        List<PublishEvent> events = eventMetadataFactory.create(event);
        if (CollectionUtils.isEmpty(events)) {
            return List.of();
        }
        return events.stream().map(message -> publish(message.toMessage())).toList();
    }

    /**
     * 内部直接发送消息到消息队列
     */
    private CompletableFuture<Void> publish(EventMessage message) {
        if (!semaphore.tryAcquire()) {
            log.warn("max concurrency reached, fallback to sync publish");
            return eventPublisher.publish(message).exceptionally(ex -> {
                log.error(ex.getMessage(), ex);
                return null;
            });
        }
        return eventPublisher.publish(message).whenComplete((v, ex) -> {
            semaphore.release();
            if (ex != null) {
                log.error(ex.getMessage(), ex);
            }
        });
    }

    /**
     * 保存消息事件内容
     *
     * @param entities 消息集合
     */
    public void initStorage(List<PublishEventEntity> entities) {
        if (CollectionUtils.isNotEmpty(entities)) {
            eventRepository.initialize(entities);
        }
    }

    /**
     * 异步批量发送消息，按 topic 分组提交，信号量满时降级为同步发送。
     *
     * @param entities 消息集合
     */
    public List<CompletableFuture<Void>> publish(List<PublishEventEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return List.of();
        }
        Map<String, List<PublishEventEntity>> grouped = entities.stream()
                                                                .collect(Collectors.groupingBy(PublishEventEntity::getName));

        return grouped.values().stream().map(batch -> {
            if (!semaphore.tryAcquire()) {
                log.warn("max concurrency reached, fallback to sync publish for {} events", batch.size());
                CompletableFuture<?>[] futures = batch.stream().map(entity -> {
                    statsManager.statsCounter(entity.getName()).recordPublishFallbackSync();
                    return doPublish(entity);
                }).toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures);
            }
            List<EventMessage> messages = batch.stream().map(PublishEventEntity::build).toList();
            return CompletableFuture.allOf(eventPublisher.publishBatch(messages).toArray(CompletableFuture[]::new))
                                    .whenComplete((v, ex) -> semaphore.release());
        }).toList();
    }

    /**
     * 发送消息并变更消息发送状态
     *
     * @param entity 消息实体信息
     */
    public CompletableFuture<Void> doPublish(PublishEventEntity entity) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        return eventPublisher.publish(entity.build()).whenComplete((v, ex) -> {
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (ex != null) {
                log.error("publish event to message queue -> id:[{}] error,taken:[{}]", entity.getId(), elapsed, ex);
                statsManager.statsCounter(entity.getName()).recordPublishFailure(elapsed);
                batchMarker.markFailed(entity);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("publish event to message queue -> id:[{}] success,taken:[{}]", entity.getId(), elapsed);
                }
                statsManager.statsCounter(entity.getName()).recordPublishSuccess(elapsed);
                batchMarker.markSucceeded(entity);
            }
        });
    }

    public boolean isConfigured() {
        return eventPublisher != null && eventRepository != null;
    }

    @Override
    public void afterSingletonsInstantiated() {
        eventPublisher = this.getBean(EventPublisher.class);
        eventRepository = this.getBean(IDomainEventRepository.class);
        eventMetadataFactory = this.getBean(EventMetadataFactory.class);
        Assert.notNull(eventMetadataFactory, "publish event factory bean not exist,please confirm right config!");
        statsManager.registerSemaphoreGauges(semaphore);
    }

    private <T> T getBean(Class<T> type) {
        try {
            return this.applicationContext.getBean(type);
        } catch (BeansException ignored) {
        }
        return null;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
