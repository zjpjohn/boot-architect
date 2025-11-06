package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class MessageQueuePublisher implements DisposableBean, ApplicationContextAware, SmartInitializingSingleton {

    private static final int KEEP_ALIVE_TIME = 1;

    private final ExecutorService        executorService;
    private       EventPublisher         eventPublisher;
    private       IDomainEventRepository eventRepository;
    private       EventMetadataFactory   eventMetadataFactory;
    private       ApplicationContext     applicationContext;

    public MessageQueuePublisher(Integer asyncThreads, Integer asyncMaxThreads, Integer asyncQueueSize) {
        this.executorService = new ThreadPoolExecutor(asyncThreads,
                                                      asyncMaxThreads,
                                                      KEEP_ALIVE_TIME,
                                                      TimeUnit.MINUTES,
                                                      new ArrayBlockingQueue<>(asyncQueueSize),
                                                      Threads.threadFactory("domain-event-publisher-"));
    }

    /**
     * 直接异步发送非事务事件消息
     *
     * @param event 事件内容
     */
    public void publish(Object event) {
        eventMetadataFactory.create(event)
                            .stream()
                            .map(PublishEvent::toMessage)
                            .forEach(message -> executorService.submit(() -> {
                                try {
                                    eventPublisher.publish(message);
                                } catch (Exception error) {
                                    log.error(error.getMessage(), error);
                                }
                            }));
    }

    /**
     * 保存消息事件内容
     *
     * @param entities 消息集合
     */
    public void initStorage(List<PublishEventEntity> entities) {
        if (!CollectionUtils.isEmpty(entities)) {
            eventRepository.initialize(entities);
        }
    }

    /**
     * 异步批量发送消息
     *
     * @param entities 消息集合
     */
    public void publish(List<PublishEventEntity> entities) {
        if (!CollectionUtils.isEmpty(entities)) {
            entities.forEach(entity -> executorService.submit(() -> doPublish(entity)));
        }
    }

    /**
     * 异步处理发送事件
     *
     * @param entity   发送事件
     * @param consumer 事件处理器
     */
    public void asyncProcess(PublishEventEntity entity, Consumer<PublishEventEntity> consumer) {
        executorService.submit(() -> consumer.accept(entity));
    }

    /**
     * 发送消息并变更消息发送状态
     *
     * @param entity 消息实体信息
     */
    public PublishResultHolder doPublish(PublishEventEntity entity) {
        Stopwatch           stopwatch = Stopwatch.createStarted();
        PublishResultHolder holder    = new PublishResultHolder();
        try {
            eventPublisher.publish(entity.build());
            if (log.isDebugEnabled()) {
                log.debug("publish event to message queue -> id:[{}] success,taken:[{}]",
                          entity.getId(),
                          stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (Throwable throwable) {
            log.error("publish event to message queue -> id:[{}] error,taken:[{}]",
                      entity.getId(),
                      stopwatch.elapsed(TimeUnit.MILLISECONDS),
                      throwable);
            holder.setSuccess(false);
            holder.setThrowable(throwable);
            try {
                eventRepository.markFailed(entity, throwable);
            } catch (Throwable error) {
                holder.setThrowable(error);
                log.error("mark publish event state -> id:[{}] error,taken:[{}]",
                          entity.getId(),
                          stopwatch.elapsed(TimeUnit.MILLISECONDS),
                          error);
            }
            holder.setTaken(stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return holder;
        }
        try {
            eventRepository.markSucceeded(entity);
            if (log.isDebugEnabled()) {
                log.debug("mark publish event state -> id:[{}] success,taken:[{}]",
                          entity.getId(),
                          stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (Throwable error) {
            holder.setSuccess(false);
            holder.setThrowable(error);
            log.error("mark publish event state -> id:[{}] error,taken:[{}]",
                      entity.getId(),
                      stopwatch.elapsed(TimeUnit.MILLISECONDS),
                      error);
        }
        holder.setTaken(stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return holder;
    }

    /**
     * 判断是否已配置消息队列
     */
    public boolean isNotNull() {
        return eventPublisher != null && eventRepository != null;
    }

    @Override
    public void destroy() throws Exception {
        this.executorService.shutdownNow();
    }

    @Override
    public void afterSingletonsInstantiated() {
        eventPublisher       = this.getBean(EventPublisher.class);
        eventRepository      = this.getBean(IDomainEventRepository.class);
        eventMetadataFactory = this.getBean(EventMetadataFactory.class);
        Assert.notNull(eventMetadataFactory, "publish event factory bean not exist,please confirm right config!");
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
