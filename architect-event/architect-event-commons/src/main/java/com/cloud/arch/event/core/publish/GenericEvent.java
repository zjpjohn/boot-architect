package com.cloud.arch.event.core.publish;

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

}
