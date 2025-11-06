package com.cloud.arch.rocket.producer.core;

import org.apache.rocketmq.common.message.Message;

import java.io.Serializable;
import java.util.Map;

public interface MessageConverter {

    /**
     * 消息转换器
     *
     * @param topic   消息topic
     * @param tag     消息过滤tag
     * @param key     消息key
     * @param payload 消息内容
     * @param headers 消息header
     */
    <T extends Serializable> Message convert(String topic, String tag, String key, T payload, Map<String, String> headers);
}
