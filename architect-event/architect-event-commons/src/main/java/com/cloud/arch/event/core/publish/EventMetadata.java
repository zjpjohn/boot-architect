package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.annotations.Publish;
import com.cloud.arch.event.annotations.Publishes;
import com.cloud.arch.event.codec.EventCodec;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class EventMetadata {

    private final HashBasedTable<String, String, PublishMetadata> remoteMetas = HashBasedTable.create();
    private final EventCodec                                      eventCodec;
    private final Class<?>                                        type;
    private       PublishMetadata                                 localMeta;

    public EventMetadata(Class<?> type, EventCodec eventCodec, StringValueResolver resolver) {
        this.type       = type;
        this.eventCodec = eventCodec;
        this.parseMeta(resolver);
    }

    public Class<?> getType() {
        return type;
    }

    public EventCodec getEventCodec() {
        return eventCodec;
    }

    public HashBasedTable<String, String, PublishMetadata> getRemoteMetas() {
        return remoteMetas;
    }

    public PublishMetadata getLocalMeta() {
        return localMeta;
    }

    /**
     * 解析领域事件注解信息
     */
    private void parseMeta(StringValueResolver resolver) {
        Set<Publish> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(type,
                                                                                         Publish.class,
                                                                                         Publishes.class);
        if (CollectionUtils.isEmpty(annotations)) {
            return;
        }
        for (Publish annotation : annotations) {
            PublishMetadata metadata = new PublishMetadata(annotation, resolver);
            //本地事件元数据,配置一个注解即可
            if (metadata.isLocal()) {
                if (localMeta != null) {
                    log.warn("local event meta best practice is only one annotation.");
                }
                localMeta = metadata;
                continue;
            }
            //远程消息事件元数据,最佳配置不需要重复配置name:filter的事件
            if (remoteMetas.contains(metadata.getName(), metadata.getFilter())) {
                log.warn("remote event meta name[{}],filter:[{}] has duplicate,please confirm your config.",
                         metadata.getName(),
                         metadata.getFilter());
                continue;
            }
            remoteMetas.put(metadata.getName(), metadata.getFilter(), metadata);
        }
    }

    public boolean isEmpty() {
        return localMeta == null && remoteMetas.isEmpty();
    }

    /**
     * 注解元数据构造本地事件
     *
     * @param result 事件数据
     */
    public PublishEvent localEvent(Object result) {
        if (localMeta == null) {
            return null;
        }
        return PublishEvent.localEvent(result, localMeta);
    }

    /**
     * 注解元数据构造远程事件
     *
     * @param result 事件数据
     */
    public List<PublishEvent> remoteEvents(String shardingKey, Object result) {
        if (remoteMetas.isEmpty()) {
            return Collections.emptyList();
        }
        return remoteMetas.cellSet()
                          .stream()
                          .map(Table.Cell::getValue)
                          .filter(Objects::nonNull)
                          .map(metadata -> PublishEvent.remoteEvent(shardingKey, metadata, eventCodec.encode(result)))
                          .collect(Collectors.toList());
    }

}
