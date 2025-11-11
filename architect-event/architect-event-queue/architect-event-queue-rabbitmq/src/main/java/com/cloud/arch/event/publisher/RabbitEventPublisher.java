package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RabbitmqProperties;
import com.cloud.arch.event.utils.RabbitEventConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitmqProperties properties, ConnectionFactory connectionFactory) {
        this.rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate.setExchange(properties.getProducer().getExchange());
    }

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     */
    @Override
    public void publish(EventMessage message) {
        Message messageExt = checkAndConvert(message);
        String  routingKey = message.getName();
        rabbitTemplate.send(routingKey, messageExt);
    }

    /**
     * 消息校验与转换
     *
     * @param message 领域事件消息内容
     */
    private Message checkAndConvert(EventMessage message) {
        Assert.state(StringUtils.hasText(message.getName()), "消息队列名称不允许为空.");
        Assert.state(StringUtils.hasText(message.getKey()), "消息标识不允许为空.");
        Assert.state(StringUtils.hasText(message.getData()), "消息内容不允许为空.");
        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                                                               .setHeader(RabbitEventConstants.RABBIT_MESSAGE_KEY,
                                                                          message.getKey())
                                                               .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                                                               .build();
        Long delay = message.getDelay();
        if (delay != null && delay > 0) {
            properties.setDelayLong(delay);
        }
        byte[] payload = message.getData().getBytes(StandardCharsets.UTF_8);
        return MessageBuilder.withBody(payload).andProperties(properties).build();
    }

}
