package com.cloud.arch.rocket.meta;


import com.cloud.arch.rocket.annotations.Sender;

public interface SenderRecogniseHandler<T extends SenderModelHandler<? extends MessageSendHandler>> {

    /**
     * 根据@Sender识别不同的消息发送模式
     *
     * @param sender 消息发送者注解
     */
    T recognise(Sender sender);

}
