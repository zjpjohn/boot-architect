package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.MessageQueuePublisher;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import com.cloud.arch.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JdbcCompensateProcessor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    private       ApplicationContext     applicationContext;
    private       MessageQueuePublisher  publisher;
    private final ExecutorService        executor;
    private final IDomainEventRepository eventRepository;

    public JdbcCompensateProcessor(IDomainEventRepository eventRepository, JdbcCompensateProperties properties) {
        this.eventRepository = eventRepository;
        JdbcCompensateProperties.Compensate cfg = properties.getCompensate();
        this.executor = new ThreadPoolExecutor(cfg.getCoreThreads(), cfg.getMaxThreads(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(cfg.getQueueSize()), Threads.threadFactory("compensate-event-"));
    }

    public void process(PublishEventEntity entity) {
        try {
            executor.submit(() -> doCompensate(entity));
        } catch (RejectedExecutionException e) {
            log.warn("compensate thread pool full, fallback to sync for event[{}]", entity.getId());
            doCompensate(entity);
        }
    }

    private void doCompensate(PublishEventEntity entity) {
        EventCompensateEntity compensate = new EventCompensateEntity();
        compensate.setId(IdWorker.nextId());
        compensate.setEventId(entity.getId());
        compensate.setShardingKey(entity.getShardingKey());
        compensate.setStartTime(System.currentTimeMillis());
        long start = System.currentTimeMillis();
        try {
            publisher.doPublish(this.calcDelay(entity));
        } catch (Throwable error) {
            compensate.setFailedMsg(extractMessage(error));
            throw error;
        } finally {
            compensate.setTaken(System.currentTimeMillis() - start);
            compensate.setGmtCreate(LocalDateTime.now());
            eventRepository.compensate(compensate);
        }
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
    private PublishEventEntity calcDelay(PublishEventEntity entity) {
        Long delay = entity.getDelay();
        if (delay != null && delay > 0) {
            Long gmtCreate = entity.getGmtCreate();
            long delta     = System.currentTimeMillis() - gmtCreate;
            entity.setDelay(delay <= delta ? 0 : delay - delta);
        }
        return entity;
    }

    @Override
    public void destroy() throws Exception {
        this.executor.shutdown();
        if (!this.executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("compensate thread pool did not terminate in 30s, forcing shutdown");
            this.executor.shutdownNow();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            this.publisher = applicationContext.getBean(MessageQueuePublisher.class);
        } catch (BeansException error) {
            log.error("message queue publisher bean not exist:", error);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
