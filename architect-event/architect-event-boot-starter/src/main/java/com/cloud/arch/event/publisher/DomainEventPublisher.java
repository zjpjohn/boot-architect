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

@Slf4j
@UtilityClass
public class DomainEventPublisher {

    /**
     * 远程消息开启标识key
     */
    private static final Integer                               ENABLE_REMOTE_KEY  = 1;
    /**
     * 空shardingKey
     */
    public static final  String                                EMPTY_SHARDING_KEY = "";
    /**
     * 当前线程本地领域事件
     */
    private static final ThreadLocal<List<Object>>             localEvents        = ThreadLocal.withInitial(LinkedList::new);
    /**
     * 暂存当前线程领域事件实体
     */
    private static final ThreadLocal<List<PublishEventEntity>> remoteEvents       = ThreadLocal.withInitial(LinkedList::new);
    /**
     * 当前线程是否注册领域事件同步器
     */
    private static final ThreadLocal<Boolean>                  synchronization    = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /**
     * 当前线程事件源分库分表shardingKey
     */
    private static final ThreadLocal<String>                   shardingContext    = ThreadLocal.withInitial(() -> EMPTY_SHARDING_KEY);
    /**
     * 是否开启远程队列标识缓存
     */
    private static final ConcurrentMap<Integer, Boolean>       remoteIndicator    = new ConcurrentHashMap<>(2);

    /**
     * 设置当前领域事件集合的shardingKey
     *
     * @param shardingKey 分库分表key
     */
    public static void shardingKey(String shardingKey) {
        Assert.state(StringUtils.isNotBlank(shardingKey), "分库分表key不允许为空.");
        shardingContext.set(shardingKey);
    }

    /**
     * 获取当前领域上下文的shardingKey
     */
    public static String shardingKey() {
        return shardingContext.get();
    }

    /**
     * 发布领域事件
     *
     * @param event 领域事件对象
     */
    public static void publish(Object event) {
        if (!synchronization.get()) {
            //判断是否配置spring事务
            boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            Assert.state(transactionActive, "领域事件未处于事务中，请配置spring事务.");
            //注册当前领域上下文的事务同步器，一次方法调用只需要注册一次
            EventPublisherSynchronization eventSynchronization = ApplicationContextHolder.getBean(
                    EventPublisherSynchronization.class);
            TransactionSynchronizationManager.registerSynchronization(eventSynchronization);
            synchronization.set(Boolean.TRUE);
        }
        //领域事件上下文存储泛化事件,泛化事件只支持远程事件
        if ((event instanceof GenericEvent genericEvent) && enableRemoteQueue()) {
            addGenericEvent(genericEvent);
            return;
        }
        //领域上下文存储领域事件
        List<PublishEvent> publishEvents = EventMetadataFactory.create(shardingKey(), event, enableRemoteQueue());
        publishEvents.forEach(DomainEventPublisher::addEvent);
    }

    /**
     * 是否开启远程事件消息队列
     */
    private static boolean enableRemoteQueue() {
        return remoteIndicator.computeIfAbsent(ENABLE_REMOTE_KEY, key -> {
            MessageQueuePublisher queuePublisher = ApplicationContextHolder.getBean(MessageQueuePublisher.class);
            return queuePublisher.isNotNull();
        });
    }

    /**
     * 获取暂存的领域事件对象
     */
    static List<PublishEventEntity> getEntities() {
        return Collections.unmodifiableList(remoteEvents.get());
    }

    /**
     * 添加泛化领域事件
     */
    private static void addGenericEvent(GenericEvent event) {
        EventCodec         eventCodec = ApplicationContextHolder.getBean(EventCodec.class);
        PublishEventEntity entity     = GenericEvent.toEntity(event, eventCodec);
        //事件自定义shardingKey为空,使用全局shardingKey
        if (StringUtils.isBlank(entity.getShardingKey())) {
            entity.setShardingKey(shardingKey());
        }
        remoteEvents.get().add(entity);
    }

    /**
     * 添加领域事件
     *
     * @param event 事件内容
     */
    private static void addEvent(PublishEvent event) {
        if (event.getMetadata().isLocal()) {
            localEvents.get().add(event.getEvent());
            return;
        }
        remoteEvents.get().add(event.toEntity());
    }

    /**
     * 获取当前领域上下文的本地领域事件集合
     */
    static List<Object> getLocals() {
        return Collections.unmodifiableList(localEvents.get());
    }

    /**
     * 获取当前领域上下文的跨应用领域事件集合
     */
    static List<PublishEventEntity> getRemotes() {
        return Collections.unmodifiableList(remoteEvents.get());
    }

    /**
     * 清空当前领域上下文
     */
    static void clear() {
        synchronization.remove();
        localEvents.remove();
        remoteEvents.remove();
        shardingContext.remove();
    }

}
