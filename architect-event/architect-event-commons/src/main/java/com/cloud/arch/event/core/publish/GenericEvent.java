package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.IdWorker;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

public interface GenericEvent {

    /**
     * 事件内容
     */
    Object event();


    /**
     * 事件主题名称
     */
    String name();

    /**
     * 事件过滤标识
     */
    default String filter() {
        return "";
    }

    /**
     * 事件分片键
     */
    default String shardingKey() {
        return "";
    }

    /**
     * 事件业务分组
     */
    default String bizGroup() {
        return "";
    }

    /**
     * 延迟事件延迟时间
     */
    default Long delay() {
        return 0L;
    }

    /**
     * 延迟事件时间单位
     */
    default TimeUnit timeUnit() {
        return TimeUnit.SECONDS;
    }

    static GenericEvent create(Object event, String name) {
        return new PublishGenericEvent(event, name);
    }

    static GenericEvent create(Object event, String name, String filter) {
        return new PublishGenericEvent(event, name, filter);
    }

    static GenericEvent create(Object event, String name, String filter, String shardingKey) {
        return new PublishGenericEvent(event, name, filter, shardingKey);
    }

    static PublishEventEntity toEntity(GenericEvent event, EventCodec codec) {
        Assert.notNull(codec, "事件序列化器不允许为空");
        Assert.notNull(event.event(), "远程事件内容不允许为空");
        Assert.state(StringUtils.hasText(event.name()), "远程事件消息主题不允许为空.");
        PublishEventEntity entity = new PublishEventEntity();
        entity.setId(IdWorker.nextId());
        entity.setBizGroup(event.bizGroup());
        entity.setName(event.name());
        entity.setFilter(event.filter());
        entity.setDelay(event.delay());
        entity.setState(EventState.INITIALIZED);
        entity.setShardingKey(event.shardingKey());
        entity.setVersion(Version.INITIAL_VERSION);
        entity.setGmtCreate(System.currentTimeMillis());
        entity.setEvent(codec.encode(event.event()));
        return entity;
    }

}
