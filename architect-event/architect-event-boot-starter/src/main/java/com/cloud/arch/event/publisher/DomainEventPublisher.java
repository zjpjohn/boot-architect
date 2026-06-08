package com.cloud.arch.event.publisher;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.commons.ApplicationContextHolder;
import com.cloud.arch.event.core.publish.EventMetadataFactory;
import com.cloud.arch.event.core.publish.GenericEvent;
import com.cloud.arch.event.core.publish.MessageQueuePublisher;
import com.cloud.arch.event.core.publish.PublishEvent;
import com.cloud.arch.event.storage.PublishEventEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 领域事件发布器，提供静态方法在事务上下文中发布本地和远程领域事件，基于 ThreadLocal 暂存事件，事务提交后统一发送。
 */
@Slf4j
@UtilityClass
public class DomainEventPublisher {

    private static final Integer                         ENABLE_REMOTE_KEY  = 1;
    public static final  String                          EMPTY_SHARDING_KEY = "";
    private static final ThreadLocal<EventContext>       CTX                = ThreadLocal.withInitial(EventContext::new);
    private static final ThreadLocal<Boolean>            synchronization    = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ConcurrentMap<Integer, Boolean> remoteIndicator    = new ConcurrentHashMap<>(2);

    /**
     * 线程内事件暂存上下文。
     */
    private static class EventContext {
        List<Object>             locals      = new LinkedList<>();
        List<PublishEventEntity> remotes     = new LinkedList<>();
        String                   shardingKey = EMPTY_SHARDING_KEY;
    }

    /**
     * 设置当前领域事件集合的shardingKey
     *
     * @param shardingKey 分库分表key
     */
    public static void shardingKey(String shardingKey) {
        Assert.state(StringUtils.isNotBlank(shardingKey), "分库分表key不允许为空.");
        CTX.get().shardingKey = shardingKey;
    }

    /**
     * 获取当前领域上下文的shardingKey
     */
    public static String shardingKey() {
        return CTX.get().shardingKey;
    }

    /**
     * 发布领域事件
     *
     * @param event 领域事件对象
     */
    public static void publish(Object event) {
        if (!synchronization.get()) {
            boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            Assert.state(transactionActive, "领域事件未处于事务中，请配置spring事务.");
            EventPublisherSynchronization eventSynchronization = ApplicationContextHolder.getBean(
                    EventPublisherSynchronization.class);
            TransactionSynchronizationManager.registerSynchronization(eventSynchronization);
            synchronization.set(Boolean.TRUE);
        }
        if ((event instanceof GenericEvent genericEvent) && enableRemoteQueue()) {
            addGenericEvent(genericEvent);
            return;
        }
        List<PublishEvent> publishEvents = EventMetadataFactory.create(shardingKey(), event, enableRemoteQueue());
        publishEvents.forEach(DomainEventPublisher::addEvent);
    }

    /**
     * 是否开启远程事件消息队列
     */
    private static boolean enableRemoteQueue() {
        return remoteIndicator.computeIfAbsent(ENABLE_REMOTE_KEY, key -> {
            MessageQueuePublisher queuePublisher = ApplicationContextHolder.getBean(MessageQueuePublisher.class);
            return queuePublisher.isConfigured();
        });
    }

    /**
     * 获取暂存的领域事件对象
     */
    static List<PublishEventEntity> getEntities() {
        return Collections.unmodifiableList(CTX.get().remotes);
    }

    /**
     * 添加泛化领域事件
     */
    private static void addGenericEvent(GenericEvent event) {
        EventCodec         eventCodec = ApplicationContextHolder.getBean(EventCodec.class);
        PublishEventEntity entity     = GenericEvent.toEntity(event, eventCodec);
        if (StringUtils.isBlank(entity.getShardingKey())) {
            entity.setShardingKey(shardingKey());
        }
        CTX.get().remotes.add(entity);
    }

    /**
     * 添加领域事件
     *
     * @param event 事件内容
     */
    private static void addEvent(PublishEvent event) {
        if (event.getMetadata().isLocal()) {
            CTX.get().locals.add(event.getEvent());
            return;
        }
        CTX.get().remotes.add(event.toEntity());
    }

    /**
     * 获取当前领域上下文的本地领域事件集合
     */
    static List<Object> getLocals() {
        return Collections.unmodifiableList(CTX.get().locals);
    }

    /**
     * 获取当前领域上下文的跨应用领域事件集合
     */
    static List<PublishEventEntity> getRemotes() {
        return Collections.unmodifiableList(CTX.get().remotes);
    }

    /**
     * 清空当前领域上下文
     */
    static void clear() {
        synchronization.remove();
        CTX.remove();
    }

}
