package com.cloud.arch.rocket.meta;

public interface SenderModelHandler<T extends MessageSendHandler> {

    /**
     * 校验发送消息元数据信息
     *
     * @param metadata 元数据信息
     */
    default void sendCheck(SenderMetadata metadata) {

    }

    /**
     * @param handler 消息发送处理器
     * @param args    发送消息方法参数
     */
    Object send(T handler, Object[] args);
}
