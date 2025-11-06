package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.PulsarMqProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PulsarEventSubscriber implements InitializingBean, DisposableBean {

    private final String                       group;
    private final EventCodec                   eventCodec;
    private final PulsarMqProperties           properties;
    private final List<SubscribeEventMetadata> metadataList;
    private final SubscribeHandler             handler;
    private final PulsarClient                 pulsarClient;

    private Consumer<String> consumer;

    public PulsarEventSubscriber(String group,
                                 EventCodec eventCodec,
                                 PulsarMqProperties properties,
                                 List<SubscribeEventMetadata> metadataList,
                                 SubscribeHandler handler,
                                 PulsarClient pulsarClient) {
        this.group        = group;
        this.eventCodec   = eventCodec;
        this.properties   = properties;
        this.metadataList = metadataList;
        this.handler      = handler;
        this.pulsarClient = pulsarClient;
    }

    public String identity() {
        return this.group;
    }

    @Override
    public void destroy() throws Exception {
        if (this.consumer != null) {
            this.consumer.close();
        }
    }

    private void createPulsarConsumer() throws Exception {
        Map<String, SubscribeEventMetadata> mappings = Maps.newHashMap();
        for (SubscribeEventMetadata metadata : this.metadataList) {
            String topic = metadata.getName();
            Assert.state(StringUtils.hasText(topic), "消息topic不允许为空.");
            if (mappings.containsKey(topic)) {
                throw new IllegalArgumentException(String.format("duplicate message topic[%s] for mapping.", topic));
            }
            mappings.put(topic, metadata);
        }
        Long    ackTimeoutMillis     = properties.getSubscriber().getAckTimeoutMillis();
        Long    acknowledgeGroupTime = properties.getSubscriber().getAcknowledgeGroupTime();
        Long    ackRedeliveryDelay   = properties.getSubscriber().getNegativeAckRedeliveryDelay();
        Integer receiverQueueSize    = properties.getSubscriber().getReceiverQueueSize();
        this.consumer = pulsarClient.newConsumer(Schema.STRING)
                                    .subscriptionName(this.group)
                                    .topics(Lists.newArrayList(mappings.keySet()))
                                    .messageListener(new EventMessageListener(eventCodec, handler, mappings))
                                    .ackTimeout(ackTimeoutMillis, TimeUnit.MILLISECONDS)
                                    .acknowledgmentGroupTime(acknowledgeGroupTime, TimeUnit.MILLISECONDS)
                                    .negativeAckRedeliveryDelay(ackRedeliveryDelay, TimeUnit.SECONDS)
                                    .receiverQueueSize(receiverQueueSize)
                                    .subscribe();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.createPulsarConsumer();
    }

}
