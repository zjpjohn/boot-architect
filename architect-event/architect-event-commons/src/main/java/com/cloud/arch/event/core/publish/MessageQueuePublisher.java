package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MessageQueuePublisher implements DisposableBean, ApplicationContextAware, SmartInitializingSingleton {

    private static final int KEEP_ALIVE_TIME = 1;

    private final ExecutorService        executorService;
    private final BatchEventMarker       batchMarker;
    private       EventPublisher         eventPublisher;
    private       IDomainEventRepository eventRepository;
    private       EventMetadataFactory   eventMetadataFactory;
    private       EventStatsManager      statsManager = EventStatsManager.disabled();
    private       ApplicationContext     applicationContext;

    public MessageQueuePublisher(Integer asyncThreads, Integer asyncMaxThreads, Integer asyncQueueSize, BatchEventMarker batchMarker) {
        this.executorService = new ThreadPoolExecutor(asyncThreads, asyncMaxThreads, KEEP_ALIVE_TIME, TimeUnit.MINUTES, new ArrayBlockingQueue<>(asyncQueueSize), Threads.threadFactory("domain-event-publisher-"));
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
    public void publish(Object event) {
        List<PublishEvent> events = eventMetadataFactory.create(event);
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        events.forEach(message -> {
            this.publish(message.toMessage());
        });
    }

    /**
     * 内部直接发送消息到消息队列
     *
     */
    private void publish(EventMessage message) {
        try {
            executorService.submit(() -> {
                try {
                    eventPublisher.publish(message);
                } catch (Exception error) {
                    log.error(error.getMessage(), error);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("thread pool full, fallback to sync publish");
            try {
                eventPublisher.publish(message);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
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
     * 异步批量发送消息，线程池满时降级为同步发送。
     *
     * @param entities 消息集合
     */
    public void publish(List<PublishEventEntity> entities) {
        if (CollectionUtils.isNotEmpty(entities)) {
            entities.forEach(entity -> {
                try {
                    executorService.submit(() -> doPublish(entity));
                } catch (RejectedExecutionException e) {
                    log.warn("thread pool full, fallback to sync publish for event[{}]", entity.getId());
                    statsManager.statsCounter(entity.getName()).recordPublishFallbackSync();
                    doPublish(entity);
                }
            });
        }
    }

    /**
     * 发送消息并变更消息发送状态
     *
     * @param entity 消息实体信息
     */
    public void doPublish(PublishEventEntity entity) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            eventPublisher.publish(entity.build());
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            statsManager.statsCounter(entity.getName()).recordPublishSuccess(elapsed);
            if (log.isDebugEnabled()) {
                log.debug("publish event to message queue -> id:[{}] success,taken:[{}]", entity.getId(), elapsed);
            }
        } catch (Throwable throwable) {
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            log.error("publish event to message queue -> id:[{}] error,taken:[{}]", entity.getId(), elapsed, throwable);
            statsManager.statsCounter(entity.getName()).recordPublishFailure(elapsed);
            batchMarker.markFailed(entity);
            if (log.isDebugEnabled()) {
                log.debug("mark publish event state -> id:[{}] error,taken:[{}]", entity.getId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
            return;
        }
        batchMarker.markSucceeded(entity);
        if (log.isDebugEnabled()) {
            log.debug("mark publish event state -> id:[{}] success,taken:[{}]", entity.getId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public boolean isConfigured() {
        return eventPublisher != null && eventRepository != null;
    }

    @Override
    public void destroy() throws Exception {
        this.executorService.shutdown();
        if (!this.executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("event publisher thread pool did not terminate in 30s, forcing shutdown");
            this.executorService.shutdownNow();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        eventPublisher = this.getBean(EventPublisher.class);
        eventRepository = this.getBean(IDomainEventRepository.class);
        eventMetadataFactory = this.getBean(EventMetadataFactory.class);
        Assert.notNull(eventMetadataFactory, "publish event factory bean not exist,please confirm right config!");
        statsManager.registerThreadPoolGauges((ThreadPoolExecutor) executorService);
    }

    private <T> T getBean(Class<T> type) {
        try {
            return this.applicationContext.getBean(type);
        } catch (BeansException ignored) {
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
