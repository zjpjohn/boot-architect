package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.lang.reflect.Method;
import java.time.Duration;


@Slf4j
public class KafkaEventListener implements AcknowledgingMessageListener<String, String> {

    public static final Method HANDLE_METHOD;

    private final SubscribeEventMetadata metadata;
    private final EventCodec             eventCodec;
    private final SubscribeHandler       subscribeHandler;

    public KafkaEventListener(SubscribeEventMetadata metadata,
                              EventCodec eventCodec,
                              SubscribeHandler subscribeHandler) {
        this.metadata         = metadata;
        this.eventCodec       = eventCodec;
        this.subscribeHandler = subscribeHandler;
    }

    static {
        try {
            HANDLE_METHOD = KafkaEventListener.class.getMethod("onMessage", ConsumerRecord.class, Acknowledgment.class);
        } catch (NoSuchMethodException error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        String eventKey = message.key();
        try {
            String body        = message.value();
            Object domainEvent = eventCodec.decode(body, metadata.getType());
            subscribeHandler.handle(eventKey, domainEvent, metadata);
            acknowledgment.acknowledge();
        } catch (Exception error) {
            log.error("kafka consume message => key[{}] error:", eventKey, error);
            acknowledgment.nack(Duration.ofSeconds(10));
        }
    }

}
