package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.google.common.collect.HashBasedTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class EventMessageListener implements MessageListener {

    private final EventCodec                                             eventCodec;
    private final SubscribeHandler                                       handler;
    private final HashBasedTable<String, String, SubscribeEventMetadata> mapping;

    public EventMessageListener(EventCodec eventCodec,
                                SubscribeHandler handler,
                                HashBasedTable<String, String, SubscribeEventMetadata> mapping) {
        this.eventCodec = eventCodec;
        this.handler    = handler;
        this.mapping    = mapping;
    }

    @Override
    public ConsumeResult consume(MessageView view) {
        String                 tag      = view.getTag().orElse("");
        SubscribeEventMetadata metadata = mapping.get(view.getTopic(), tag);
        if (metadata == null) {
            return ConsumeResult.FAILURE;
        }
        String           body     = StandardCharsets.UTF_8.decode(view.getBody()).toString();
        Iterator<String> iterator = view.getKeys().iterator();
        String           eventKey = iterator.hasNext() ? iterator.next() : "";
        if (StringUtils.isBlank(eventKey)) {
            return ConsumeResult.FAILURE;
        }
        try {
            Object event = eventCodec.decode(body, metadata.getType());
            handler.handle(eventKey, event, metadata);
            return ConsumeResult.SUCCESS;
        } catch (Exception error) {
            log.error("subscriber handle event message => key:[{}] error.", eventKey, error);
        }
        return ConsumeResult.FAILURE;
    }

}
