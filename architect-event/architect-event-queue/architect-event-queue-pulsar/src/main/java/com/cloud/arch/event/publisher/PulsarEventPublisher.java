package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventMetadata;
import com.cloud.arch.event.core.publish.EventMetadataFactory;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.PulsarMqProperties;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PulsarEventPublisher
        implements EventPublisher, SmartInitializingSingleton, DisposableBean, EmbeddedValueResolverAware {

    private final Map<String, Producer<String>> producerHolder = Maps.newHashMap();
    private final PulsarClient                  pulsarClient;
    private final PulsarMqProperties            properties;
    private       StringValueResolver           resolver;

    public PulsarEventPublisher(PulsarClient pulsarClient, PulsarMqProperties properties) {
        this.pulsarClient = pulsarClient;
        this.properties   = properties;
    }

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     */
    @Override
    public void publish(EventMessage message) {
        Assert.state(StringUtils.isNotBlank(message.getName()), "消息topic不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getData()), "消息内容不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getKey()), "消息业务key不允许为空.");
        try {
            Producer<String>            producer = producerHolder.get(message.getName());
            TypedMessageBuilder<String> builder  = producer.newMessage().key(message.getKey()).value(message.getData());
            Long                        delay    = message.getDelay();
            if (delay != null && delay > 0) {
                builder.deliverAfter(delay, TimeUnit.MILLISECONDS);
            }
            builder.send();
        } catch (PulsarClientException error) {
            log.error(error.getMessage(), error);
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    @Override
    public void destroy() throws Exception {
        producerHolder.values().forEach(Producer::closeAsync);
    }

    @Override
    @SneakyThrows
    public void afterSingletonsInstantiated() {
        Map<String, Class<?>> mapping = this.getTopicEventMapping();
        for (String topic : mapping.keySet()) {
            Integer sendTimeout        = properties.getPublisher().getSendTimeout();
            Integer maxPendingMessages = properties.getPublisher().getMaxPendingMessages();
            Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
                                                    .topic(topic)
                                                    .sendTimeout(sendTimeout, TimeUnit.SECONDS)
                                                    .maxPendingMessages(maxPendingMessages)
                                                    .create();
            producerHolder.put(topic, producer);
        }
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 获取元数据的topic-event映射关系
     */
    private Map<String, Class<?>> getTopicEventMapping() {
        Map<Class<?>, EventMetadata> metaMap = EventMetadataFactory.getMetaMap();
        Map<String, Class<?>>        mapping = Maps.newHashMap();
        Collection<EventMetadata>    values  = metaMap.values();
        for (EventMetadata metadata : values) {
            Set<String> keySets = metadata.getRemoteMetas().rowKeySet();
            for (String keySet : keySets) {
                String topic = resolver.resolveStringValue(keySet);
                if (mapping.containsKey(topic)) {
                    throw new IllegalArgumentException(String.format("duplicate message topic[%s] for mapping.",
                                                                     topic));
                }
                mapping.put(topic, metadata.getType());
            }
        }
        return mapping;
    }
}
