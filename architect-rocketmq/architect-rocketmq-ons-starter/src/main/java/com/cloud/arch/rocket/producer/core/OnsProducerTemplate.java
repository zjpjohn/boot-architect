package com.cloud.arch.rocket.producer.core;

import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.producer.tx.OnsTransactionState;
import com.cloud.arch.rocket.producer.tx.TransactionBusinessHandler;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.utils.RocketOnsConstants;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class OnsProducerTemplate implements DisposableBean, SmartInitializingSingleton, ApplicationContextAware {

    private final OnsQueueProperties  properties;
    private final Serialize           serialize;
    private       ApplicationContext  context;
    private       TransactionChecker  checker;
    private       Producer            producer;
    private       OrderProducer       orderProducer;
    private       TransactionProducer transactionProducer;

    public OnsProducerTemplate(OnsQueueProperties properties, Serialize serialize) {
        this.properties = properties;
        this.serialize  = serialize;
    }

    public <T extends Serializable> SendResult send(String topic, T payload) {
        return this.send(topic, null, payload);
    }

    public <T extends Serializable> SendResult send(String topic, String tag, T payload) {
        return this.send(topic, tag, null, payload);
    }

    public <T extends Serializable> SendResult send(String topic, String tag, String key, T payload) {
        return this.producer.send(checkAndSet(topic, tag, key, payload));
    }

    public <T extends Serializable> void sendAsync(String topic, T payload, SendCallback callback) {
        this.sendAsync(topic, null, payload, callback);
    }

    public <T extends Serializable> void sendAsync(String topic, String tag, T payload, SendCallback callback) {
        this.sendAsync(topic, tag, null, payload, callback);
    }

    public <T extends Serializable> void sendAsync(String topic,
                                                   String tag,
                                                   String key,
                                                   T payload,
                                                   SendCallback callback) {
        this.producer.sendAsync(checkAndSet(topic, tag, key, payload), callback);
    }

    public <T extends Serializable> void sendOneway(String topic, T payload) {
        this.sendOneway(topic, null, null, payload);
    }

    public <T extends Serializable> void sendOneway(String topic, String tag, T payload) {
        this.sendOneway(topic, tag, null, payload);
    }

    public <T extends Serializable> void sendOneway(String topic, String tag, String key, T payload) {
        this.producer.sendOneway(checkAndSet(topic, tag, key, payload));
    }

    public <T extends Serializable> void sendDelay(String topic,
                                                   String tag,
                                                   String key,
                                                   Collection<Long> delays,
                                                   T payload) {
        delays.forEach(delay -> {
            Message message = checkAndSet(topic, tag, key, payload);
            message.setStartDeliverTime(delay);
            this.producer.send(message);
        });
    }

    public <T extends Serializable> SendResult sendDelay(String topic, String tag, String key, Long delay, T payload) {
        Message message = checkAndSet(topic, tag, key, payload);
        message.setStartDeliverTime(delay);
        return this.producer.send(message);
    }

    public <T extends Serializable> SendResult sendOrder(String topic, T payload, String shardingKey) {
        return this.sendOrder(topic, null, payload, shardingKey);
    }

    public <T extends Serializable> SendResult sendOrder(String topic, String tag, T payload, String shardingKey) {
        return this.orderProducer.send(checkAndSet(topic, tag, null, payload), shardingKey);
    }

    public <T extends Serializable> SendResult sendOrder(String topic,
                                                         String tag,
                                                         String key,
                                                         T payload,
                                                         String shardingKey) {
        Preconditions.checkArgument(this.properties.getProducer().isOrdered(), "未开启顺序消息，请先开启顺序消息");
        Preconditions.checkArgument(StringUtils.isNotBlank(shardingKey), "顺序消息shardingKey不允许为空");
        return this.orderProducer.send(checkAndSet(topic, tag, key, payload), shardingKey);
    }

    public <T extends Serializable> SendResult sendTransaction(String topic,
                                                               String tag,
                                                               String key,
                                                               T payload,
                                                               TransactionBusinessHandler handler) {
        Message message = checkAndSet(topic, tag, key, payload);
        return this.transactionProducer.send(message, (msg, arg) -> {
            OnsTransactionState transactionState = OnsTransactionState.UNKNOWN;
            try {
                handler.handle(msg);
                transactionState = OnsTransactionState.COMMIT;
            } catch (Exception e) {
                log.error("handle transaction message msgId:{},exception:", msg.getMsgID(), e);
                transactionState = OnsTransactionState.ROLLBACK;
            }
            return transactionState.getStatus();
        }, null);
    }

    /**
     * @param topic   消息主题
     * @param tag     消息tag
     * @param key     消息业务key
     * @param payload 消息内容
     */
    private <T extends Serializable> Message checkAndSet(String topic, String tag, String key, T payload) {
        Preconditions.checkArgument(StringUtils.isNotBlank(topic), "消息topic不允许为空");
        Preconditions.checkArgument(payload != null, "消息内容不允许为空");
        String tagRegex = Optional.ofNullable(tag).orElse("*");
        return new Message(topic, tagRegex, key, serialize.serialize(payload));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(this.producer).ifPresent(Producer::shutdown);
        Optional.ofNullable(this.orderProducer).ifPresent(OrderProducer::shutdown);
        Optional.ofNullable(this.transactionProducer).ifPresent(TransactionProducer::shutdown);
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            Properties props = new Properties();
            props.put(PropertyKeyConst.AccessKey, this.properties.getAccessKey());
            props.put(PropertyKeyConst.SecretKey, this.properties.getSecretKey());
            props.put(PropertyKeyConst.NAMESRV_ADDR, this.properties.getOnsAddress());
            //默认启用普通消息生产者
            producer = ONSFactory.createProducer(props);
            producer.start();
            //顺序消息生产者启用
            if (this.properties.getProducer().isOrdered()) {
                orderProducer = ONSFactory.createOrderProducer(props);
                orderProducer.start();
            }
            //事物消息生产者启用
            if (this.properties.getProducer().isTransaction()) {
                this.checker = Preconditions.checkNotNull(this.context.getBean(RocketOnsConstants.CHECK_SERVICE_BEAN_NAME,
                                                                               TransactionChecker.class));
                final LocalTransactionChecker transactionChecker = Preconditions.checkNotNull(this.context.getBean(
                        RocketOnsConstants.LOCAL_TRANSACTION_CHECKER_BEAN_NAME,
                        LocalTransactionChecker.class));
                this.transactionProducer = ONSFactory.createTransactionProducer(props, transactionChecker);
                this.transactionProducer.start();
            }
        } catch (BeansException e) {
            log.error("初始化ons消息队列生产者失败:", e);
            throw new RuntimeException("初始化ons消息队列生产者失败.", e);
        }
    }

}
