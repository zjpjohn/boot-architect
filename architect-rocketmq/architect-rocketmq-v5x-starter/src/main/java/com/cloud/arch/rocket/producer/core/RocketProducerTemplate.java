package com.cloud.arch.rocket.producer.core;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.domain.DelayMessage;
import com.cloud.arch.rocket.utils.RocketmqCloudException;
import com.cloud.arch.rocket.utils.RocketmqConstants;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import com.cloud.arch.utils.IdWorker;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RocketProducerTemplate implements InitializingBean, DisposableBean {

    /**
     * 属性配置
     */
    private final RocketmqProperties   properties;
    /**
     * 消息转换器
     */
    private final MessageConverter     converter;
    /**
     * 普通消息生产者
     */
    private       DefaultMQProducer    producer;
    /**
     * 顺序消息队列选择器
     */
    private final MessageQueueSelector queueSelector = new SelectMessageQueueByHash();


    public RocketProducerTemplate(RocketmqProperties properties, MessageConverter converter) {
        this.properties = properties;
        this.converter  = converter;
    }

    /**
     * 发送receive模式消息
     *
     * @param topic    消息topic
     * @param tag      消息过滤tag
     * @param payload  消息内容
     * @param callback 发送receive回调
     */
    public <T extends Serializable> void sendAndReceive(String topic, String tag, T payload, RequestCallback callback) {
        sendAndReceive(topic, tag, null, 0, payload, callback);
    }

    /**
     * 发送receive模式消息
     *
     * @param topic    消息topic
     * @param tag      消息过滤tag
     * @param key      消息业务标识
     * @param timeout  发送超时时间
     * @param payload  消息内容
     * @param callback 发送receive回调
     */
    public <T extends Serializable> void sendAndReceive(String topic,
                                                        String tag,
                                                        String key,
                                                        long timeout,
                                                        T payload,
                                                        RequestCallback callback) {
        sendAndReceive(topic, tag, key, null, timeout, payload, callback);
    }

    /**
     * 发送顺序消息receive模式
     *
     * @param topic    消息topic
     * @param tag      消息tag
     * @param key      消息业务key
     * @param hashKey  顺序hashKey
     * @param payload  消息内容
     * @param callback 消息receive回调
     */
    public <T extends Serializable> void sendAndReceive(String topic,
                                                        String tag,
                                                        String hashKey,
                                                        String key,
                                                        T payload,
                                                        RequestCallback callback) {
        sendAndReceive(topic, tag, hashKey, key, 0, payload, callback);
    }


    /**
     * 发送顺序消息receive模式
     *
     * @param topic    消息topic
     * @param tag      消息tag
     * @param key      消息业务Key
     * @param hashKey  顺序hashKey
     * @param timeout  发送超时时间
     * @param payload  消息内容
     * @param callback 消息receive回调
     */
    public <T extends Serializable> void sendAndReceive(String topic,
                                                        String tag,
                                                        String hashKey,
                                                        String key,
                                                        long timeout,
                                                        T payload,
                                                        RequestCallback callback) {
        if (timeout <= 0) {
            timeout = properties.getProducer().getSendMessageTimeout();
        }
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            if (StringUtils.isNotBlank(hashKey)) {
                producer.request(message, callback, timeout);
                return;
            }
            producer.request(message, queueSelector, callback, timeout);
        } catch (Exception e) {
            log.error("send request message failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 同步发送普通消息
     *
     * @param topic   消息topic
     * @param tag     消息过滤tag
     * @param key     消息业务key
     * @param payload 消息内容
     */
    public <T extends Serializable> SendResult syncSend(String topic, String tag, String key, T payload) {
        return syncSend(topic, tag, key, properties.getProducer().getSendMessageTimeout(), payload);
    }

    /**
     * 同步发送普通消息
     *
     * @param topic   消息topic
     * @param tag     消息tag
     * @param key     消息业务key
     * @param timeout 发送超时时间
     * @param payload 消息内容
     */
    public <T extends Serializable> SendResult syncSend(String topic, String tag, String key, long timeout, T payload) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            return producer.send(message, timeout);
        } catch (Exception e) {
            log.error("sync send message failed,topic:{},tag:{},message.keys:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 同步批量发送消息
     *
     * @param topic    消息topic
     * @param tag      消息tag
     * @param messages 消息集合
     */
    public <T extends Serializable> SendResult syncBatchSend(String topic, String tag, Collection<T> messages) {
        return syncBatchSend(topic, tag, properties.getProducer().getSendMessageTimeout(), messages);
    }

    /**
     * 同步批量发送消息
     *
     * @param topic    消息topic
     * @param tag      消息过滤tag
     * @param timeout  消息发送超时时间
     * @param payloads 消息集合
     */
    public <T extends Serializable> SendResult syncBatchSend(String topic,
                                                             String tag,
                                                             long timeout,
                                                             Collection<T> payloads) {
        Collection<Message> messages = checkAndSet(topic, tag, payloads);
        try {
            return producer.send(messages, timeout);
        } catch (Exception e) {
            log.error("sync send batch messages failed,topic:{},tag:{},message.size:{}", topic, tag, messages.size());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 同步发送延迟消息
     *
     * @param topic    消息topic
     * @param tag      消息过滤tag
     * @param delivers 消息延迟到指定时间点集合
     * @param payload  消息内容
     * @param bizKey   业务标识
     */
    public <T extends Serializable> SendResult syncDeliverSend(String topic,
                                                               String tag,
                                                               String bizKey,
                                                               Set<Long> delivers,
                                                               T payload) {
        return syncDeliverSend(topic, tag, bizKey, properties.getProducer().getSendMessageTimeout(), delivers, payload);
    }

    /**
     * 同步发送延迟消息
     *
     * @param topic    消息topic
     * @param tag      消息tag
     * @param timeout  消息发送超时时间
     * @param delivers 消息延迟到指定时间点集合
     * @param payload  消息内容
     * @param bizKey   消息业务标识
     */
    public <T extends Serializable> SendResult syncDeliverSend(String topic,
                                                               String tag,
                                                               String bizKey,
                                                               long timeout,
                                                               Set<Long> delivers,
                                                               T payload) {
        Message message = checkAndSet(topic, tag, delivers, payload, bizKey);
        try {
            return producer.send(message, timeout);
        } catch (Exception e) {
            log.error("sync deliver send message failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 同步发送延迟消息
     *
     * @param topic   消息topic
     * @param tag     消息过滤tag
     * @param delays  延迟间隔时间集合
     * @param payload 消息内容
     * @param bizKey  业务标识
     */
    public <T extends Serializable> SendResult syncDelaySend(String topic,
                                                             String tag,
                                                             String bizKey,
                                                             Set<Long> delays,
                                                             T payload) {
        return syncDelaySend(topic, tag, bizKey, properties.getProducer().getSendMessageTimeout(), delays, payload);
    }

    /**
     * 同步发送延迟消息
     *
     * @param topic   消息topic
     * @param tag     消息过滤tag
     * @param timeout 发送延迟时间
     * @param delays  延迟间隔时间集合
     * @param payload 消息内容
     * @param bizKey  业务标识
     */
    public <T extends Serializable> SendResult syncDelaySend(String topic,
                                                             String tag,
                                                             String bizKey,
                                                             long timeout,
                                                             Set<Long> delays,
                                                             T payload) {
        long      currentTime = System.currentTimeMillis();
        Set<Long> delivers    = delays.stream().map(v -> currentTime + v).collect(Collectors.toSet());
        Message   message     = checkAndSet(topic, tag, delivers, payload, bizKey);
        try {
            return producer.send(message, timeout);
        } catch (Exception e) {
            log.error("sync delay send failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 同步发送顺序消息
     *
     * @param topic   消息topic
     * @param tag     过滤tag
     * @param key     消息业务key
     * @param hashKey 顺序hashKey
     * @param payload 消息内容
     */
    public <T extends Serializable> SendResult syncOrderlySend(String topic,
                                                               String tag,
                                                               String hashKey,
                                                               String key,
                                                               T payload) {
        return syncOrderlySend(topic, tag, hashKey, key, properties.getProducer().getSendMessageTimeout(), payload);
    }

    /**
     * 同步发送顺序消息
     *
     * @param topic   消息topic
     * @param tag     过滤tag
     * @param key     消息业务key
     * @param hashKey 顺序hashKey
     * @param timeout 发送超时时间
     * @param payload 消息内容
     */
    public <T extends Serializable> SendResult syncOrderlySend(String topic,
                                                               String tag,
                                                               String hashKey,
                                                               String key,
                                                               long timeout,
                                                               T payload) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            return producer.send(message, queueSelector, hashKey, timeout);
        } catch (Exception e) {
            log.error("sync orderly send failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }


    /**
     * 异步发送普通消息
     *
     * @param topic    消息topic
     * @param tag      过滤tag
     * @param key      消息业务key
     * @param payload  消息内容
     * @param callback 发送回调
     */
    public <T extends Serializable> void asyncSend(String topic,
                                                   String tag,
                                                   String key,
                                                   T payload,
                                                   SendCallback callback) {
        asyncSend(topic, tag, key, properties.getProducer().getSendMessageTimeout(), payload, callback);
    }

    /**
     * 异步发送普通消息
     *
     * @param topic    消息topic
     * @param tag      消息过滤tag
     * @param key      消息业务key
     * @param timeout  发送超时时间
     * @param payload  消息内容
     * @param callback 发送回调
     */
    public <T extends Serializable> void asyncSend(String topic,
                                                   String tag,
                                                   String key,
                                                   long timeout,
                                                   T payload,
                                                   SendCallback callback) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            producer.send(message, callback, timeout);
        } catch (Exception e) {
            log.error("async send failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
        }
    }

    /**
     * 异步发送顺序消息
     *
     * @param topic    消息topic
     * @param tag      过滤tag
     * @param key      消息业务key
     * @param hashKey  顺序hashKey
     * @param payload  消息内容
     * @param callback 消息回调
     */
    public <T extends Serializable> void asyncOrderlySend(String topic,
                                                          String tag,
                                                          String hashKey,
                                                          String key,
                                                          T payload,
                                                          SendCallback callback) {
        asyncOrderlySend(topic, tag, hashKey, key, properties.getProducer().getSendMessageTimeout(), payload, callback);
    }

    /**
     * 异步发送顺序消息
     *
     * @param topic    消息topic
     * @param tag      过滤tag
     * @param key      消息业务key
     * @param hashKey  顺序hashKey
     * @param timeout  发送超时时间
     * @param payload  消息内容
     * @param callback 消息回调
     */
    public <T extends Serializable> void asyncOrderlySend(String topic,
                                                          String tag,
                                                          String hashKey,
                                                          String key,
                                                          long timeout,
                                                          T payload,
                                                          SendCallback callback) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            producer.send(message, queueSelector, hashKey, callback, timeout);
        } catch (Exception e) {
            log.error("async orderly send failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 发送OneWay模式消息
     *
     * @param topic   消息topic
     * @param tag     过滤tag
     * @param payload 消息内容
     */
    public <T extends Serializable> void sendOneWay(String topic, String tag, String key, T payload) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            producer.sendOneway(message);
        } catch (Exception e) {
            log.error("send one way failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 发送OneWay顺序消息
     *
     * @param topic   消息topic
     * @param tag     过滤tag
     * @param hashKey 顺序hashKey
     * @param payload 消息内容
     */
    public <T extends Serializable> void sendOrderlyOneWay(String topic,
                                                           String tag,
                                                           String key,
                                                           String hashKey,
                                                           T payload) {
        Message message = checkAndSet(topic, tag, payload, key);
        try {
            producer.sendOneway(message, queueSelector, hashKey);
        } catch (Exception e) {
            log.error("send orderly one way failed,topic:{},tag:{},message.key:{}", topic, tag, message.getKeys());
            throw new RocketmqCloudException(e.getMessage(), e);
        }
    }

    /**
     * 校验并构造消息
     *
     * @param topic   消息topic
     * @param tag     过滤tag
     * @param key     消息业务Key
     * @param payload 消息内容
     */
    private <T extends Serializable> Message checkAndSet(String topic, String tag, T payload, String key) {
        Assert.state(StringUtils.isNotBlank(topic), "消息topic主题为空");
        Assert.notNull(payload, "消息内容为空");
        return converter.convert(topic, tag, key, payload, null);
    }

    /**
     * 校验并构造批量消息
     *
     * @param topic    消息topic
     * @param tag      过滤tag
     * @param payloads 消息内容集合
     */
    private <T extends Serializable> Collection<Message> checkAndSet(String topic, String tag, Collection<T> payloads) {
        Assert.state(StringUtils.isNotBlank(topic), "消息topic主题为空");
        Assert.state(!CollectionUtils.isEmpty(payloads), "消息内容集合为空");
        Map<String, String> headers = Maps.newHashMap();
        return payloads.stream().map(v -> {
            headers.put(RocketmqConstants.KEYS, IdWorker.uuid());
            return converter.convert(topic, tag, null, v, headers);
        }).collect(Collectors.toList());
    }

    /**
     * 校验并构造延迟消息
     *
     * @param topic    消息topic
     * @param tag      过滤tag
     * @param delivers 延迟时间集合
     * @param payload  消息内容
     * @param bizKey   业务标识
     */
    private <T extends Serializable> Message checkAndSet(String topic,
                                                         String tag,
                                                         Set<Long> delivers,
                                                         T payload,
                                                         String bizKey) {
        Assert.state(StringUtils.isNotBlank(topic), "消息topic主题为空");
        Assert.state(!CollectionUtils.isEmpty(delivers), "延迟时间集合为空");
        Assert.notNull(payload, "消息内容为空");
        Map<String, String> headers = Maps.newHashMap();
        DelayMessage<T>     content = new DelayMessage<>(topic, tag, payload, delivers, bizKey);
        return converter.convert(properties.getProducer().getDelayTopic(), tag, bizKey, content, headers);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RocketmqProperties.RocketmqProducerProperties producerCfg = properties.getProducer();
        this.producer = RocketmqUtils.createProducer(producerCfg.getGroup(),
                                                     properties.getAccessKey(),
                                                     properties.getSecretKey(),
                                                     producerCfg.isEnableTrace(),
                                                     producerCfg.getTraceTopic());
        this.producer.setNamesrvAddr(properties.getNameSrv());
        String accessChannel = properties.getAccessChannel();
        if (StringUtils.isNotBlank(accessChannel)) {
            this.producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        this.producer.setSendMsgTimeout(producerCfg.getSendMessageTimeout());
        this.producer.setMaxMessageSize(producerCfg.getMaxMessageSize());
        this.producer.setRetryTimesWhenSendFailed(producerCfg.getRetryTimesWhenSendFailed());
        this.producer.setRetryAnotherBrokerWhenNotStoreOK(producerCfg.isRetryNextServer());
        this.producer.setCompressMsgBodyOverHowmuch(producerCfg.getCompressMsgBodyThrottle());
        this.producer.setRetryTimesWhenSendAsyncFailed(producerCfg.getRetryTimesWhenSendAsyncFailed());
        this.producer.start();
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(producer).ifPresent(DefaultMQProducer::shutdown);
    }

}
