package com.cloud.arch.event.core.publish;

import java.util.concurrent.TimeUnit;

public class PublishGenericEvent implements GenericEvent {

    private final Object   event;
    private final String   name;
    private final String   filter;
    private final String   shadingKey;
    private final String   bizGroup;
    private final Long     delay;
    private final TimeUnit timeUnit;

    public PublishGenericEvent(Object event, String name) {
        this(event, name, "");
    }

    public PublishGenericEvent(Object event, String name, String filter) {
        this(event, name, filter, "");
    }

    public PublishGenericEvent(Object event, String name, String filter, String shadingKey) {
        this(event, name, filter, shadingKey, "");
    }

    public PublishGenericEvent(Object event, String name, String filter, String shadingKey, String bizGroup) {
        this(event, name, filter, shadingKey, bizGroup, 0L, TimeUnit.SECONDS);
    }

    public PublishGenericEvent(Object event, String name, String filter, String shadingKey, String bizGroup, Long delay, TimeUnit timeUnit) {
        this.event      = event;
        this.name       = name;
        this.filter     = filter;
        this.shadingKey = shadingKey;
        this.bizGroup   = bizGroup;
        this.delay      = delay;
        this.timeUnit   = timeUnit;
    }

    /**
     * 事件内容
     */
    @Override
    public Object event() {
        return this.event;
    }

    /**
     * 事件主题名称
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * 事件过滤标识
     */
    @Override
    public String filter() {
        return this.filter;
    }

    /**
     * 事件分片键
     */
    @Override
    public String shardingKey() {
        return this.shadingKey;
    }

    /**
     * 事件业务分组
     */
    @Override
    public String bizGroup() {
        return this.bizGroup;
    }

    /**
     * 延迟事件延迟时间
     */
    @Override
    public Long delay() {
        return this.delay;
    }

    /**
     * 延迟事件时间单位
     */
    @Override
    public TimeUnit timeUnit() {
        return this.timeUnit;
    }
}
