package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventConcurrentlyListener implements MessageListenerConcurrently {

    private static final int delayLevelWhenNextConsume = 0;

    private final SubscribeHandler                              subscribeHandler;
    private final EventCodec                                    eventCodec;
    private final Table<String, String, SubscribeEventMetadata> registry;

    public EventConcurrentlyListener(Table<String, String, SubscribeEventMetadata> registry,
                                     SubscribeHandler subscribeHandler,
                                     EventCodec eventCodec) {
        this.registry         = registry;
        this.subscribeHandler = subscribeHandler;
        this.eventCodec       = eventCodec;
    }

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt messageExt : msgs) {
            Stopwatch              stopwatch = Stopwatch.createStarted();
            String                 topic     = messageExt.getTopic();
            String                 tags      = messageExt.getTags();
            SubscribeEventMetadata metadata  = registry.get(topic, tags);
            if (metadata == null) {
                log.warn("not found consume topic:[{}],tag:[{}] real message payload type.", topic, tags);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            try {
                String event       = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                Object domainEvent = eventCodec.decode(event, metadata.getType());
                subscribeHandler.handle(messageExt.getKeys(), domainEvent, metadata);
                log.info("concurrently consume topic:[{}],tags:[{}}], message id:[{}] success,taken:[{}]ms", topic, tags, messageExt.getMsgId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                log.warn("concurrently consume message topic:[{}],message id:[{}] error,taken:[{}]ms", messageExt.getTopic(), messageExt.getMsgId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                context.setDelayLevelWhenNextConsume(delayLevelWhenNextConsume);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

}
