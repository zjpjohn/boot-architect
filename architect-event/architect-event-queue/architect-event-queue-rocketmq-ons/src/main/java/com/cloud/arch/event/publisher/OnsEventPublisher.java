package com.cloud.arch.event.publisher;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.OnsQueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class OnsEventPublisher implements EventPublisher, DisposableBean, SmartInitializingSingleton {

    public static final String ONS_ALL_TAG_REGEX = "*";

    private final OnsQueueProperties properties;
    private       Producer           producer;

    public OnsEventPublisher(OnsQueueProperties properties) {
        this.properties = properties;
    }

    /**
     * 发布跨应用领域事件
     *
     * @param message 领域事件消息
     */
    @Override
    public void publish(EventMessage message) {
        Message onsMessage = checkAnConvert(message);
        this.producer.send(onsMessage);
    }

    /**
     * 事件消息校验转换
     *
     * @param message 事件消息内容
     */
    private Message checkAnConvert(EventMessage message) {
        Assert.state(StringUtils.isNotBlank(message.getName()), "消息topic不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getData()), "消息内容不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getKey()), "消息业务key不允许为空");
        //强制设置消息过滤tag不允许为空且不能为'*'
        String messageFilter = message.getFilter();
        Assert.state(StringUtils.isNotBlank(messageFilter)
                     && !ONS_ALL_TAG_REGEX.equals(messageFilter), "消息过滤tag不允许为空，请根据业务设置具体过滤tag");
        byte[]  payload    = message.getData().getBytes(StandardCharsets.UTF_8);
        Message onsMessage = new Message(message.getName(), messageFilter, message.getKey(), payload);
        Long    delay      = message.getDelay();
        if (delay != null && delay > 0) {
            onsMessage.setStartDeliverTime(System.currentTimeMillis() + delay);
        }
        return onsMessage;
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(producer).ifPresent(Producer::shutdown);
    }

    @Override
    public void afterSingletonsInstantiated() {
        Properties props = new Properties();
        props.put(PropertyKeyConst.NAMESRV_ADDR, properties.getOnsAddress());
        props.put(PropertyKeyConst.AccessKey, properties.getAccessKey());
        props.put(PropertyKeyConst.SecretKey, properties.getSecretKey());
        props.put(PropertyKeyConst.GROUP_ID, properties.getPublisher().getGroup());
        props.put(PropertyKeyConst.EXACTLYONCE_DELIVERY, properties.getPublisher().isExactlyOnceDelivery());
        props.put(PropertyKeyConst.SendMsgTimeoutMillis, properties.getPublisher().getSendTimeoutMillis());
        this.producer = ONSFactory.createProducer(props);
        this.producer.start();
    }

}
