package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

public class KafkaSubscriberProcessor extends AbsSubscriberProcessor implements ApplicationContextAware {

    private final EventCodec                                              eventCodec;
    private final KafkaProperties                                         properties;
    private final ConcurrentKafkaListenerContainerFactory<String, String> containerFactory;
    private final KafkaListenerEndpointRegistry                           endpointRegistry;
    private final MessageHandlerMethodFactory                             handlerMethodFactory;

    private ApplicationContext context;

    public KafkaSubscriberProcessor(EventCodec eventCodec,
                                    KafkaProperties properties,
                                    KafkaListenerEndpointRegistry endpointRegistry,
                                    MessageHandlerMethodFactory handlerMethodFactory,
                                    ConcurrentKafkaListenerContainerFactory<String, String> containerFactory) {
        this.eventCodec           = eventCodec;
        this.properties           = properties;
        this.containerFactory     = containerFactory;
        this.endpointRegistry     = endpointRegistry;
        this.handlerMethodFactory = handlerMethodFactory;
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    @Override
    public void registerListeners(List<SubscribeEventMetadata> metadataList) {
        SubscribeHandler subscribeHandler = this.context.getBean(SubscribeHandler.class);
        // 注册事件订阅监听器
        metadataList.stream().distinct().forEach(metaData -> this.registryHandler(metaData, subscribeHandler));
    }

    private void registryHandler(SubscribeEventMetadata registration, SubscribeHandler subscribeHandler) {
        String topic = registration.getName();
        if (StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("消息队列topic为空，请配置topic信息.");
        }
        String                                      groupId  = this.resolveGroup(registration);
        MethodKafkaListenerEndpoint<String, String> endpoint = new MethodKafkaListenerEndpoint<>();
        endpoint.setGroupId(groupId);
        endpoint.setId(registration.getName());
        endpoint.setTopics(registration.getName());
        endpoint.setMethod(KafkaEventListener.HANDLE_METHOD);
        endpoint.setMessageHandlerMethodFactory(handlerMethodFactory);
        endpoint.setBean(new KafkaEventListener(registration, eventCodec, subscribeHandler));
        this.endpointRegistry.registerListenerContainer(endpoint, this.containerFactory, true);
    }

    private String resolveGroup(SubscribeEventMetadata registration) {
        String group = registration.getGroup();
        if (org.apache.commons.lang3.StringUtils.isBlank(group)) {
            group = properties.getConsumer().getGroupId();
        }
        Assert.state(org.apache.commons.lang3.StringUtils.isNotBlank(group), "请配置消费者群组");
        return group;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
