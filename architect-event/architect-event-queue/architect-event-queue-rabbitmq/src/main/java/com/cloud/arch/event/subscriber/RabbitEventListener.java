package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.utils.RabbitEventConstants;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.nio.charset.StandardCharsets;

@Slf4j
public class RabbitEventListener implements ChannelAwareMessageListener {

    private final SubscribeEventMetadata metadata;
    private final EventCodec             eventCodec;
    private final SubscribeHandler       subscribeHandler;

    public RabbitEventListener(SubscribeEventMetadata metadata,
                               EventCodec eventCodec,
                               SubscribeHandler subscribeHandler) {
        this.metadata         = metadata;
        this.eventCodec       = eventCodec;
        this.subscribeHandler = subscribeHandler;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        MessageProperties properties  = message.getMessageProperties();
        long              deliveryTag = properties.getDeliveryTag();
        String            eventKey    = properties.getHeader(RabbitEventConstants.RABBIT_MESSAGE_KEY);
        try {
            String payload     = new String(message.getBody(), StandardCharsets.UTF_8);
            Object domainEvent = eventCodec.decode(payload, metadata.getType());
            subscribeHandler.handle(eventKey, domainEvent, metadata);
            channel.basicAck(deliveryTag, false);
        } catch (Exception error) {
            log.error("rabbitmq consume message => key[{}] error:", eventKey, error);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
