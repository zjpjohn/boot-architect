package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.RocketmqProperties;
import com.cloud.arch.event.publisher.RocketEventPublisher;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class RocketEventSubscriber implements InitializingBean, DisposableBean {

    public static final String COMPOSITE_TAG_DELIMITER = "||";

    private final RocketmqProperties           properties;
    private final String                       group;
    private final SubscribeHandler             subscribeHandler;
    private final EventCodec                   eventCodec;
    private final List<SubscribeEventMetadata> registrations;

    private Table<String, String, SubscribeEventMetadata> metas;
    private DefaultMQPushConsumer                         consumer;
    private EventConcurrentlyListener                     listener;

    public RocketEventSubscriber(String group, EventCodec eventCodec, RocketmqProperties properties, List<SubscribeEventMetadata> registrations, SubscribeHandler subscribeHandler) {
        Assert.state(StringUtils.isNotBlank(group), "consumer group不允许为空.");
        this.group = group;
        this.properties = properties;
        this.eventCodec = eventCodec;
        this.registrations = registrations;
        this.subscribeHandler = subscribeHandler;
    }

    public String identity() {
        return group;
    }

    /**
     * 解析监听元数据以及创建监听器
     */
    private void parseRegistryAndCreate() {
        this.metas = HashBasedTable.create();
        for (SubscribeEventMetadata registration : this.registrations) {
            // 消息topic解析校验
            String topic = registration.getName();
            Assert.state(StringUtils.isNotBlank(topic), "消息topic不允许为空.");
            // 消息tag解析校验
            String tagRegex = registration.getFilter();
            // 消息过滤tag配置规则:不允许为空、不允许为'*'、不允许包含'||'
            boolean tagValidation = StringUtils.isNotBlank(tagRegex) &&
                                    !RocketEventPublisher.ROCKETMQ_ALL_TAG_REGEX.equals(tagRegex) &&
                                    !tagRegex.contains(COMPOSITE_TAG_DELIMITER);
            Assert.state(tagValidation, "请配置具有业务意义的消息tag.");
            Assert.state(!metas.contains(topic, tagRegex), "同一topic消息主题下不允许配置相同tag.");
            // 缓存监听类型元数据
            metas.put(topic, tagRegex, registration);
        }
        this.listener = new EventConcurrentlyListener(metas, subscribeHandler, eventCodec);
    }

    /**
     * 创建消费者实例
     */
    private void createConsumer() {
        try {
            RPCHook                             rpcHook    = properties.rpcHook();
            RocketmqProperties.RocketmqConsumer subscriber = properties.getSubscriber();
            this.consumer = new DefaultMQPushConsumer(group, rpcHook, new AllocateMessageQueueAveragely(), subscriber.isEnableTrace(), subscriber.getTraceTopic());
            this.consumer.setConsumeThreadMax(subscriber.getConsumerThreadMax());
            int consumerThreadMin = Math.min(subscriber.getConsumerThreadMax(), subscriber.getConsumerThreadMin());
            this.consumer.setConsumeThreadMin(consumerThreadMin);
            this.consumer.setNamesrvAddr(properties.getNameSrv());
            this.consumer.setAccessChannel(properties.accessChannel());
            this.consumer.setConsumeTimeout(subscriber.getConsumerTimeout());
            this.consumer.setMessageModel(MessageModel.CLUSTERING);
            this.consumer.registerMessageListener(this.listener);
            for (String topic : metas.rowKeySet()) {
                Map<String, SubscribeEventMetadata> column       = metas.column(topic);
                String                              compositeTag = String.join(COMPOSITE_TAG_DELIMITER, column.keySet());
                this.consumer.subscribe(topic, compositeTag);
            }
            this.consumer.start();
        } catch (MQClientException e) {
            log.error("创建RocketMq消费者异常:", e);
            throw new RuntimeException("RocketMQ consumer start failed", e);
        }
    }

    @Override
    public void destroy() {
        Optional.ofNullable(consumer).ifPresent(DefaultMQPushConsumer::shutdown);
    }

    @Override
    public void afterPropertiesSet() {
        // 解析并创建监听器
        this.parseRegistryAndCreate();
        // 创建消费者并启动
        this.createConsumer();
    }

}
