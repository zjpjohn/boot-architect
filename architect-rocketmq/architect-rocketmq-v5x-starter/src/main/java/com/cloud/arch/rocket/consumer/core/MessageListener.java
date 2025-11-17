package com.cloud.arch.rocket.consumer.core;

import org.apache.rocketmq.common.message.MessageExt;

public interface MessageListener {

    /**
     * 单条消息处理
     * 请不要吞掉异常，抛出异常后框架会根据配置进行重试
     *
     * @param message 消息内容
     *
     */
    void handle(MessageExt message) throws Exception;

}
