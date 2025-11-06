package com.cloud.arch.event.core.subscribe;

public interface SubscribeHandler {

    /**
     * 领域事件业务处理
     *
     * @param eventKey 领域事件key
     * @param event    领域事件
     * @param metadata 订阅事件元数据信息
     */
    void handle(String eventKey, Object event, SubscribeEventMetadata metadata);
}
