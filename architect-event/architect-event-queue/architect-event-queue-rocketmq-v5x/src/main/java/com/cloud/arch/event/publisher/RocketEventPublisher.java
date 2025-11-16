package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RocketmqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
     *
     * @param message 事件消息
     */
    @Override
    public void publish(EventMessage message) {
        try {
            Message messageExt = this.checkAndConvert(message);
            this.producer.send(messageExt, properties.getPublisher().getSendMessageTimeout());
        } catch (Exception error) {
            log.error("发送rocketmq消息topic:[{}],key:[{}]异常:", message.getName(), message.getKey(), error);
            throw new RuntimeException(error);
        }
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
        Assert.state(StringUtils.isNotBlank(messageFilter) && !ROCKETMQ_ALL_TAG_REGEX.equals(messageFilter),
                     "消息过滤tag不允许为空，请根据业务设置具体过滤tag");
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
            boolean enableAcl = StringUtils.isNotBlank(properties.getAccessKey())
                    && StringUtils.isNotBlank(properties.getSecretKey());
            RPCHook rpcHook = null;
            if (enableAcl) {
                rpcHook = new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(),
                                                                      properties.getSecretKey()));
            }
            String group = properties.getPublisher().getGroup();
            this.producer = new DefaultMQProducer(group,
                                                  rpcHook,
                                                  properties.getPublisher().isEnableTrace(),
                                                  properties.getPublisher().getTraceTopic());
            this.producer.setSendMessageWithVIPChannel(enableAcl);
            this.producer.setNamesrvAddr(properties.getNameSrv());
            this.producer.setSendMsgTimeout(properties.getPublisher().getSendMessageTimeout());
            this.producer.setMaxMessageSize(properties.getPublisher().getMaxMessageSize());
            this.producer.setRetryTimesWhenSendFailed(properties.getPublisher().getRetryTimesWhenSendFailed());
            this.producer.setRetryAnotherBrokerWhenNotStoreOK(properties.getPublisher().isRetryNextServer());
            this.producer.setCompressMsgBodyOverHowmuch(properties.getPublisher().getCompressMsgBodyThrottle());
            String accessChannel = properties.getAccessChannel();
            if (StringUtils.isNotBlank(accessChannel)) {
                this.producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
            }
            this.producer.start();
        } catch (MQClientException e) {
            log.error("创建rocketmq生产者异常:", e);
        }
    }
}
