package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import com.cloud.arch.trigger.BufferedTrigger;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 独立攒批发送组件，不依赖 {@link MessageQueuePublisher}。
 * 通过 {@code com.cloud.event.publisher.batch.enabled=true} 开关启用，
 * 启用后 afterCommit 走 {@link #publish(List)} → {@link BufferedTrigger} drain → 按 topic 分组 → 异步发送。
 */
@Slf4j
public class BatchMessagePublisher implements DisposableBean, ApplicationContextAware, SmartInitializingSingleton {

    private final BufferedTrigger<PublishEventEntity> trigger;
    private final ExecutorService                     drainExecutor;
    private final BatchEventMarker                    batchMarker;
    private       IDomainEventRepository              eventRepository;
    private       EventPublisher                      eventPublisher;
    private       EventMetadataFactory                eventMetadataFactory;
    private       EventStatsManager                   statsManager = EventStatsManager.disabled();
    private       ApplicationContext                  applicationContext;

    public BatchMessagePublisher(int batchSize, long drainTimeoutMs, int queueCapacity, BatchEventMarker batchMarker) {
        this.batchMarker = batchMarker;
        this.drainExecutor = Executors.newSingleThreadExecutor(Threads.threadFactory("outbox-stealer"));
        this.trigger = BufferedTrigger.<PublishEventEntity>builder()
                                      .executor(drainExecutor)
                                      .batchSize(batchSize)
                                      .queue(new LinkedBlockingQueue<>(queueCapacity))
                                      .timeout(Duration.ofMillis(drainTimeoutMs))
                                      .consumer(this::processBatch)
                                      .build();
    }

    public void setEventStatsManager(EventStatsManager statsManager) {
        if (statsManager != null) {
            this.statsManager = statsManager;
        }
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
     * 直接异步发送非事务事件
     */
    public List<CompletableFuture<Void>> publish(Object event) {
        List<PublishEvent> events = eventMetadataFactory.create(event);
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }
        List<EventMessage> messages = events.stream().map(PublishEvent::toMessage).toList();
        return eventPublisher.publishBatch(messages);
    }

    /**
     * 非阻塞入队，由 BufferedTrigger 内部 SleepyTask 按 batchSize + timeout 攒批后消费。
     */
    public void publish(List<PublishEventEntity> entities) {
        trigger.publish(entities);
    }

    /**
     * 攒批回调：按 topic 分组 → 异步发送，逐条记录指标与标记。
     */
    private void processBatch(List<PublishEventEntity> batch) {
        Map<String, List<PublishEventEntity>> grouped = CollectionUtils.groupBy(batch, PublishEventEntity::getName);
        grouped.forEach((topic, entities) -> {
            List<EventMessage>            messages  = entities.stream().map(PublishEventEntity::build).toList();
            List<CompletableFuture<Void>> futures   = eventPublisher.publishBatch(messages);
            Stopwatch                     stopwatch = Stopwatch.createStarted();
            for (int i = 0; i < futures.size(); i++) {
                PublishEventEntity entity = entities.get(i);
                futures.get(i).whenComplete((v, ex) -> {
                    long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                    if (ex != null) {
                        statsManager.statsCounter(entity.getName()).recordPublishFailure(elapsed);
                        batchMarker.markFailed(entity);
                    } else {
                        statsManager.statsCounter(entity.getName()).recordPublishSuccess(elapsed);
                        batchMarker.markSucceeded(entity);
                    }
                });
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((v, ex) -> {
                if (ex != null) {
                    log.error("batch publish error for topic:[{}], count:[{}]", topic, entities.size(), ex);
                }
            });
        });
    }

    public boolean isConfigured() {
        return eventPublisher != null && eventRepository != null;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.eventPublisher = this.getBean(EventPublisher.class);
        this.eventRepository = this.getBean(IDomainEventRepository.class);
        this.eventMetadataFactory = this.getBean(EventMetadataFactory.class);
        Assert.notNull(eventMetadataFactory, "publish event factory bean not exist,please confirm right config.");
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        trigger.shutdown();
        drainExecutor.shutdownNow();
    }

    private <T> T getBean(Class<T> type) {
        try {
            return this.applicationContext.getBean(type);
        } catch (BeansException ignored) {
        }
        return null;
    }

}
