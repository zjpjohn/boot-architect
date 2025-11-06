package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.annotations.Publish;
import com.cloud.arch.event.annotations.Publishes;
import com.cloud.arch.event.codec.EventCodec;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EventMetadataFactory implements EmbeddedValueResolverAware, InitializingBean {
    /**
     * Events领域事件注解
     */
    private static final Class<Publishes>             PUBLISHES_ANNOTATION = Publishes.class;
    /**
     * Event领域事件注解
     */
    private static final Class<Publish>               PUBLISH_ANNOTATION   = Publish.class;
    /**
     * 事件注解元数据缓存
     */
    private static final Map<Class<?>, EventMetadata> METADATA_CACHE       = Maps.newConcurrentMap();

    private final EventCodec          eventCodec;
    private       StringValueResolver valueResolver;

    public EventMetadataFactory(EventCodec eventCodec) {
        this.eventCodec = eventCodec;
    }

    /**
     * 获取事件元数据
     */
    public static Map<Class<?>, EventMetadata> getMetaMap() {
        return Collections.unmodifiableMap(METADATA_CACHE);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.scanCandidateEvents();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.valueResolver = resolver;
    }

    /**
     * 扫描@Publishes和@Publish注解类
     * 构建领域事件元数据集合
     */
    public void scanCandidateEvents() {
        Set<Class<?>> eventClass = Sets.newHashSet();
        eventClass.addAll((Set<Class<?>>) ClassIndex.getAnnotated(PUBLISHES_ANNOTATION));
        eventClass.addAll((Set<Class<?>>) ClassIndex.getAnnotated(PUBLISH_ANNOTATION));
        for (Class<?> clazz : eventClass) {
            METADATA_CACHE.put(clazz, new EventMetadata(clazz, eventCodec, valueResolver));
        }
    }

    /**
     * 构造远程事件集合
     * 应用场景为直接发送远程领域事件
     *
     * @param event 领域事件
     */
    public List<PublishEvent> create(Object event) {
        EventMetadata metadata = METADATA_CACHE.get(event.getClass());
        if (metadata == null) {
            return Collections.emptyList();
        }
        return metadata.remoteEvents(null, event);
    }

    /**
     * 构造领域事件
     *
     * @param shardingKey  shardingKey分库分表情况下使用
     * @param event        领域事件数据
     * @param enableRemote 是否已启用消息队列
     */
    public static List<PublishEvent> create(String shardingKey, Object event, boolean enableRemote) {
        List<PublishEvent> events   = Lists.newArrayList();
        EventMetadata      metadata = METADATA_CACHE.get(event.getClass());
        if (metadata == null) {
            return events;
        }
        //本地领域事件
        PublishEvent localEvent = metadata.localEvent(event);
        if (localEvent != null) {
            events.add(localEvent);
        }
        if (enableRemote) {
            //远程领域事件
            List<PublishEvent> remoteEvents = metadata.remoteEvents(shardingKey, event);
            if (!CollectionUtils.isEmpty(remoteEvents)) {
                events.addAll(remoteEvents);
            }
        }
        return events;
    }

}
