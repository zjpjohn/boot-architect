package com.cloud.arch.rocket.consumer.spring;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.consumer.core.ListenerMetadata;
import com.cloud.arch.rocket.consumer.core.MessageListener;
import com.cloud.arch.rocket.consumer.core.impl.SingleMessageListener;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.utils.MessageModeUtils;
import com.cloud.arch.rocket.utils.RocketmqConstants;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RocketmqConsumerContainer implements InitializingBean, DisposableBean {

    /**
     * 消息消费重试策略
     * -1-不重试，消息直接进入DLQ队列
     * 0-broker控制重试频率
     * >0-客户端控制重试策略
     */
    private static final int                    delayLevelWhenNextConsume = 0;
    private final        RocketmqProperties     properties;
    private final        String                 group;
    private final        MessageModel           model;
    private final        List<ListenerMetadata> metaList;
    private final        Serialize              serialize;

    private DefaultMQPushConsumer                            consumer;
    private HashBasedTable<String, String, ListenerMetadata> metaTable;

    public RocketmqConsumerContainer(String group,
                                     MessageModel model,
                                     Serialize serialize,
                                     RocketmqProperties properties,
                                     List<ListenerMetadata> metaList) {
        this.group      = group;
        this.model      = model;
        this.properties = properties;
        this.metaList   = metaList;
        this.serialize  = serialize;
    }

    public String identity() {
        return group + "_" + model.getModel();
    }

    /**
     * 解析消费者监听器元数据
     */
    private void parseAndRegisterListeners() throws MQClientException {
        this.metaTable = HashBasedTable.create();
        metaList.forEach(meta -> {
            String topic = meta.getTopic(), tag = meta.getTag();
            Assert.state(!metaTable.contains(topic, tag), "同一个topic消费主题下不允许存在相同tag的Listener.");
            metaTable.put(topic, tag, meta);
        });
        MessageListener messageListener = new SingleMessageListener(this.metaTable, serialize);
        this.consumer.setMessageListener(new DefaultMessageListenerConcurrently(messageListener));
        for (String topic : this.metaTable.rowKeySet()) {
            Map<String, ListenerMetadata> column = metaTable.column(topic);
            String compositeTag = String.join(RocketmqConstants.ROCKET_TAG_DELIMITER, column.keySet());
            this.consumer.subscribe(topic, compositeTag);
        }
    }

    /**
     * 创建并启动消费者实例
     */
    private void createAndStartConsumer() throws Exception {
        RPCHook rpcHook = null;
        if (StringUtils.isNotBlank(properties.getAccessKey()) && StringUtils.isNotBlank(properties.getSecretKey())) {
            rpcHook
                    = new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey()));
        }
        this.consumer
                = new DefaultMQPushConsumer(group, rpcHook, new AllocateMessageQueueAveragely(), properties.getConsumer()
                                                                                                           .isEnableTrace(), properties.getConsumer()
                                                                                                                                       .getTraceTopic());
        this.consumer.setConsumeThreadMax(properties.getConsumer().getConsumerThreadMax());
        if (this.properties.getConsumer().getConsumerThreadMax() < this.consumer.getConsumeThreadMin()) {
            this.consumer.setConsumeThreadMin(this.properties.getConsumer().getConsumerThreadMax());
        }
        this.consumer.setNamesrvAddr(properties.getNameSrv());
        this.consumer.setAccessChannel(AccessChannel.valueOf(properties.getAccessChannel()));
        this.consumer.setConsumeTimeout(properties.getConsumer().getConsumerTimeout());
        this.consumer.setMessageModel(MessageModeUtils.model(this.model));
        this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        //解析注册监听器
        this.parseAndRegisterListeners();
        //启动消费者实例
        this.consumer.start();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.createAndStartConsumer();
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(consumer).ifPresent(DefaultMQPushConsumer::shutdown);
    }

    /**
     * concurrently消费模式消息处理器
     */
    public static class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {

        private final MessageListener messageListener;

        public DefaultMessageListenerConcurrently(MessageListener messageListener) {
            this.messageListener = messageListener;
        }

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            for (MessageExt messageExt : msgs) {
                try {
                    messageListener.handle(messageExt);
                } catch (Exception e) {
                    log.warn("consume message failed,messageId:[{}],topic:[{}],tag:[{}],reconsumeTimes:{}", messageExt.getMsgId(), messageExt.getTopic(), messageExt.getTags(), messageExt.getReconsumeTimes());
                    context.setDelayLevelWhenNextConsume(delayLevelWhenNextConsume);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            log.info("consume topic[{}] messages size:{} success,taken time {} ms.", context.getMessageQueue()
                                                                                            .getTopic(), msgs.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

}
