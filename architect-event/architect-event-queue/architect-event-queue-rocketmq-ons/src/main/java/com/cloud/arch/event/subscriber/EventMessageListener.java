package com.cloud.arch.event.subscriber;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class EventMessageListener implements MessageListener {

    private final Map<String, SubscribeEventMetadata> mapping;
    private final EventCodec       eventCodec;
    private final SubscribeHandler subscribeHandler;

    public EventMessageListener(Map<String, SubscribeEventMetadata> mapping,
                                EventCodec eventCodec,
                                SubscribeHandler subscribeHandler) {
        this.mapping          = mapping;
        this.eventCodec       = eventCodec;
        this.subscribeHandler = subscribeHandler;
    }

    @Override
    public Action consume(Message message, ConsumeContext context) {
        String                 tag      = message.getTag();
        SubscribeEventMetadata metadata = mapping.get(tag);
        if (metadata == null) {
            log.warn("event subscriber[{}:{}] info not exist.", message.getTopic(), tag);
            return Action.ReconsumeLater;
        }
        String eventKey = message.getKey();
        String event    = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            Object domainEvent = eventCodec.decode(event, metadata.getType());
            subscribeHandler.handle(eventKey, domainEvent, metadata);
            return Action.CommitMessage;
        } catch (Exception error) {
            log.error("handle subscribe event message => key:[{}] error.", eventKey, error);
            return Action.ReconsumeLater;
        }
    }
}
