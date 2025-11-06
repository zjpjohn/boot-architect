package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;

import java.util.Map;

@Slf4j
public class EventMessageListener implements MessageListener<String> {

    private static final long serialVersionUID = 8509018542326795429L;

    private final EventCodec                          eventCodec;
    private final SubscribeHandler                    handler;
    private final Map<String, SubscribeEventMetadata> mappings;

    public EventMessageListener(EventCodec eventCodec,
                                SubscribeHandler handler,
                                Map<String, SubscribeEventMetadata> mappings) {
        this.eventCodec = eventCodec;
        this.handler    = handler;
        this.mappings   = mappings;
    }

    @Override
    public void received(Consumer<String> consumer, Message<String> message) {
        String                 topicName = message.getTopicName();
        SubscribeEventMetadata metadata  = mappings.get(topicName);
        if (metadata == null) {
            consumer.negativeAcknowledge(message);
            return;
        }
        String eventKey = message.getKey();
        if (StringUtils.isBlank(eventKey)) {
            consumer.negativeAcknowledge(message);
            return;
        }
        try {
            Object event = this.eventCodec.decode(message.getValue(), metadata.getType());
            this.handler.handle(eventKey, event, metadata);
            consumer.acknowledge(message);
        } catch (Exception error) {
            log.error("subscriber handle event message => key [{}] error.", eventKey, error);
            consumer.negativeAcknowledge(message);
        }
    }

}
