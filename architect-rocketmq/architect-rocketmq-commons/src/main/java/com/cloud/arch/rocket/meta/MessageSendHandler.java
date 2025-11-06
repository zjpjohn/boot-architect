package com.cloud.arch.rocket.meta;

public interface MessageSendHandler {

    /**
     * 发送消息代理接口
     *
     * @param args 发送消息方法参数
     */
    Object invoke(Object[] args) throws Throwable;

    /**
     * 校验消息发送参数校验
     */
    void validate();

}
