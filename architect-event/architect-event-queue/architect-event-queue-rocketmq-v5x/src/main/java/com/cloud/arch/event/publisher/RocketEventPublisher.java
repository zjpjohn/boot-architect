package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RocketmqProperties;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RocketEventPublisher implements EventPublisher, DisposableBean, SmartInitializingSingleton {

    public static final String ROCKETMQ_ALL_TAG_REGEX = "*";

    private final RocketmqProperties properties;
    private       DefaultMQProducer  producer;

    public RocketEventPublisher(RocketmqProperties properties) {
        this.properties = properties;
    }

    /**
     * 发布跨应用领域事件
     */
    @Override
    public CompletableFuture<Void> publish(EventMessage message) {
        return send(checkAndConvert(message));
    }

    /**
     * 批量发送：普通消息走批量发送，延迟消息退化循环发送
     */
    @Override
    public List<CompletableFuture<Void>> publishBatch(List<EventMessage> messages) {
        if (properties.getPublisher().isEnableBatch()) {
            return nativeBatch(messages);
        }
        return this.sendBatch(messages);
    }

    /**
     * 发送结果处理
     */
    private void callbackHandle(CompletableFuture<Void>[] results, List<Integer> indices, Throwable throwable) {
        if (throwable != null) {
            for (int idx : indices) {
                results[idx].completeExceptionally(throwable);
            }
            return;
        }
        for (int idx : indices) {
            results[idx].complete(null);
        }
    }

    /**
     * 对已转换的 {@link Message} 执行单条异步发送。
     */
    private CompletableFuture<Void> send(Message msg) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            producer.send(msg, new SendCallback() {
                public void onSuccess(SendResult result) {
                    future.complete(null);
                }

                public void onException(Throwable e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException(e));
        }
        return future;
    }

    /**
     * 批量发送消息
     */
    private List<CompletableFuture<Void>> sendBatch(List<EventMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }
        List<Message> innerMessages = messages.stream().map(this::checkAndConvert).toList();
        return innerMessages.stream().map(this::send).toList();
    }

    /**
     * 原生批量发送
     */
    @SuppressWarnings("unchecked")
    private List<CompletableFuture<Void>> nativeBatch(List<EventMessage> messages) {
        int                       size       = messages.size();
        CompletableFuture<Void>[] results    = new CompletableFuture[size];
        List<Message>             normalList = Lists.newArrayList();
        List<Integer>             indices    = Lists.newArrayList();

        for (int index = 0; index < size; index++) {
            EventMessage message = messages.get(index);
            if (message.isDelay()) {
                results[index] = publish(message);
            } else {
                indices.add(index);
                normalList.add(checkAndConvert(message));
                results[index] = new CompletableFuture<>();
            }
        }
        if (CollectionUtils.isNotEmpty(normalList)) {
            try {
                producer.send(normalList, new SendCallback() {
                    public void onSuccess(SendResult sendResult) {
                        callbackHandle(results, indices, null);
                    }

                    public void onException(Throwable error) {
                        callbackHandle(results, indices, error);
                    }
                });
            } catch (Exception error) {
                callbackHandle(results, indices, error);
            }
        }
        return Arrays.asList(results);
    }

    /**
     * 领域事件消息校验转换
     *
     * @param message 领域事件消息
     */
    private Message checkAndConvert(EventMessage message) {
        Assert.state(StringUtils.isNotBlank(message.getName()), "消息队列topic不允许为空.");
        Assert.state(StringUtils.isNotBlank(message.getData()), "消息内容不允许为空.");
        Assert.state(StringUtils.isNotBlank(message.getKey()), "消息唯一标识key不允许为空.");
        //强制设置消息过滤tag不允许为空且不能为'*'
        String messageFilter = message.getFilter();
        Assert.state(StringUtils.isNotBlank(messageFilter) &&
                     !ROCKETMQ_ALL_TAG_REGEX.equals(messageFilter), "消息过滤tag不允许为空，请根据业务设置具体过滤tag");
        byte[] payload = message.getData().getBytes(StandardCharsets.UTF_8);
        return new Message(message.getName(), messageFilter, message.getKey(), payload);
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(producer).ifPresent(DefaultMQProducer::shutdown);
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            RPCHook                             rpcHook   = properties.rpcHook();
            RocketmqProperties.RocketmqProducer publisher = properties.getPublisher();
            this.producer = new DefaultMQProducer(publisher.getGroup(), rpcHook, publisher.isEnableTrace(), publisher.getTraceTopic());
            this.producer.setSendMessageWithVIPChannel(rpcHook != null);
            this.producer.setNamesrvAddr(properties.getNameSrv());
            this.producer.setSendMsgTimeout(publisher.getSendMessageTimeout());
            this.producer.setMaxMessageSize(publisher.getMaxMessageSize());
            this.producer.setRetryTimesWhenSendFailed(publisher.getRetryTimesWhenSendFailed());
            this.producer.setRetryAnotherBrokerWhenNotStoreOK(publisher.isRetryNextServer());
            this.producer.setCompressMsgBodyOverHowmuch(publisher.getCompressMsgBodyThrottle());
            this.producer.setAccessChannel(properties.accessChannel());
            this.producer.start();
        } catch (MQClientException e) {
            log.error("创建RocketMq生产者异常:", e);
            throw new RuntimeException("RocketMQ producer start failed", e);
        }
    }
}
