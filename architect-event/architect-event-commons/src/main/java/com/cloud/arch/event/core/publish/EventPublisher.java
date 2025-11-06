package com.cloud.arch.event.core.publish;

public interface EventPublisher {

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     */
    void publish(EventMessage message);

}
