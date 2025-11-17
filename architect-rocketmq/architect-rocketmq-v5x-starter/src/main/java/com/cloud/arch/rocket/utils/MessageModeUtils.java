package com.cloud.arch.rocket.utils;


import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

public class MessageModeUtils {

    public static MessageModel model(com.cloud.arch.rocket.domain.MessageModel model) {
        return switch (model) {
            case CLUSTERING -> MessageModel.CLUSTERING;
            case BROADCASTING -> MessageModel.BROADCASTING;
            default -> throw new IllegalArgumentException("消费消息模式错误.");
        };
    }

}
