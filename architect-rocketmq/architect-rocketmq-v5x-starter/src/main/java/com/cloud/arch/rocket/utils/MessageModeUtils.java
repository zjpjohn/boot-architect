package com.cloud.arch.rocket.utils;


import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

public class MessageModeUtils {

    public static MessageModel model(com.cloud.arch.rocket.domain.MessageModel model) {
        switch (model) {
            case CLUSTERING:
                return MessageModel.CLUSTERING;
            case BROADCASTING:
                return MessageModel.BROADCASTING;
            default:
                throw new IllegalArgumentException("消费消息模式错误.");
        }
    }

}
