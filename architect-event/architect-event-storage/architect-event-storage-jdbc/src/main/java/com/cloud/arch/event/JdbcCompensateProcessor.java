package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.MessageQueuePublisher;
import com.cloud.arch.event.core.publish.PublishResultHolder;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDateTime;

@Slf4j
public class JdbcCompensateProcessor implements ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext applicationContext;

    private       MessageQueuePublisher  publisher;
    private final IDomainEventRepository eventRepository;

    public JdbcCompensateProcessor(IDomainEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void process(PublishEventEntity entity) {
        publisher.asyncProcess(entity, this::doCompensate);
    }

    private void doCompensate(PublishEventEntity entity) {
        EventCompensateEntity compensate = new EventCompensateEntity();
        compensate.setId(IdWorker.nextId());
        compensate.setEventId(entity.getId());
        compensate.setShardingKey(entity.getShardingKey());
        //发送开始时间
        compensate.setStartTime(System.currentTimeMillis());
        //消息补偿发送
        PublishResultHolder holder = publisher.doPublish(this.calcDelay(entity));
        //发送失败错误信息
        compensate.setFailedMsg(holder.getErrorMsg());
        //发送耗时
        compensate.setTaken(holder.getTaken());
        //记录创建时间
        compensate.setGmtCreate(LocalDateTime.now());
        eventRepository.compensate(compensate);
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
