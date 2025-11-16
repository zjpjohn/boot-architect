package com.cloud.arch.rocket.consumer.core;

import org.apache.rocketmq.common.message.MessageExt;

public interface MessageListener {

    /**
     * 单条消息处理
     *
     * @param message 消息内容
     *
     * @throws Exception
     */
    void handle(MessageExt message) throws Exception;
}
