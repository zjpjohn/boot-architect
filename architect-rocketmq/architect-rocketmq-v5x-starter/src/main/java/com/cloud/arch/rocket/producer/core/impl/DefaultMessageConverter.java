package com.cloud.arch.rocket.producer.core.impl;

import com.cloud.arch.rocket.producer.core.MessageConverter;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class DefaultMessageConverter implements MessageConverter {

    private final Serialize serialize;

    public DefaultMessageConverter(Serialize serialize) {
        this.serialize = serialize;
    }

    /**
     * 消息转换器
     *
     * @param topic   消息topic
     * @param tag     消息过滤tag
     * @param key     消息key
     * @param payload 消息内容
     * @param headers 消息header
     */
    @Override
    public <T extends Serializable> Message convert(String topic,
                                                    String tag,
                                                    String key,
                                                    T payload,
                                                    Map<String, String> headers) {
        //消息内容为字符串
        byte[]  body    = serialize.serialize(payload);
        Message message = new Message(topic, tag, body);
        if (StringUtils.isNotBlank(key)) {
            message.setKeys(key);
        }
        if (!CollectionUtils.isEmpty(headers)) {
            String keys = headers.get(RocketmqUtils.KEYS);
            if (StringUtils.isNotBlank(keys)) {
                message.setKeys(keys);
            }
            int    flag    = 0;
            String flagStr = Optional.ofNullable(headers.get(RocketmqUtils.FLAG)).orElse("0");
            try {
                flag = Integer.parseInt(flagStr);
            } catch (NumberFormatException e) {
                if (log.isInfoEnabled()) {
                    log.info("flag must be int,flagStr:{}", flagStr);
                }
            }
            message.setFlag(flag);
            Boolean waitStoreOkStr = Optional.ofNullable(headers.get(RocketmqUtils.WAIT_STORE_MSG_OK))
                                             .map(Boolean::valueOf)
                                             .orElse(true);
            message.setWaitStoreMsgOK(Boolean.TRUE.equals(waitStoreOkStr));
            headers.entrySet()
                   .stream()
                   .filter(entry -> !Objects.equals(entry.getKey(), RocketmqUtils.FLAG)
                           && !Objects.equals(entry.getKey(), RocketmqUtils.WAIT_STORE_MSG_OK)
                           && !MessageConst.STRING_HASH_SET.contains(entry.getKey())) // exclude "FLAG", "WAIT_STORE_MSG_OK"
                   .forEach(entry -> message.putUserProperty(entry.getKey(), String.valueOf(entry.getValue())));
        }
        return message;
    }
}
