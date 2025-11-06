package com.cloud.arch.event.publisher;

import com.cloud.arch.event.commons.ApplicationContextHolder;
import com.cloud.arch.event.core.publish.MessageQueuePublisher;
import com.cloud.arch.event.storage.PublishEventEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.List;

@Slf4j
public class EventPublisherSynchronization implements TransactionSynchronization {

    private final MessageQueuePublisher queuePublisher;

    public EventPublisherSynchronization(MessageQueuePublisher queuePublisher) {
        this.queuePublisher = queuePublisher;
    }

    /**
     * 事务提交前发送本地事件和保存远程领域事件
     * 注：beforeCommit方法仍在事务内，如果抛出异常仍会使事务回滚
     */
    @Override
    public void beforeCommit(boolean readOnly) {
        //远程领域事件本地持久化
        if (queuePublisher.isNotNull()) {
            List<PublishEventEntity> remoteEvents = DomainEventPublisher.getRemotes();
            queuePublisher.initStorage(remoteEvents);
        }
        //本地领域事件直接发送不进行持久化，直接通过spring event进行发布
        DomainEventPublisher.getLocals().forEach(ApplicationContextHolder::publishEvent);
    }

    /**
     * 事务提交成功后发送远程领域事件
     */
    @Override
    public void afterCommit() {
        try {
            if (queuePublisher.isNotNull()) {
                List<PublishEventEntity> entities = DomainEventPublisher.getEntities();
                //将领域事件推送到消息队列
                queuePublisher.publish(entities);
            }
        } catch (Exception error) {
            //此处是线程池爆满而产生的错误，吞掉错误防止返回给前端
            log.error("async publish domain event to message queue error:", error);
        }
    }

    /**
     * 事务处理完成清空当前领域上下文
     */
    @Override
    public void afterCompletion(int status) {
        DomainEventPublisher.clear();
    }

}
