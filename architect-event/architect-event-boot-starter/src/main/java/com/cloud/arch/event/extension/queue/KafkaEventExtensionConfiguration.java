package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.publisher.KafkaEventPublisher;
import com.cloud.arch.event.subscriber.KafkaSubscriberProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureAfter(KafkaAutoConfiguration.class)
@ConditionalOnClass(name = "com.cloud.arch.event.props.KafkaEventProperties")
public class KafkaEventExtensionConfiguration {

    @Bean
    @ConditionalOnBean(EventPublisher.class)
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(KafkaTemplate<String, String> template) {
        return new KafkaEventPublisher(template);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public KafkaSubscriberProcessor kafkaSubscriberProcessor(EventCodec eventCodec,
                                                             KafkaProperties properties,
                                                             KafkaListenerEndpointRegistry endpointRegistry,
                                                             MessageHandlerMethodFactory handlerMethodFactory,
                                                             ConcurrentKafkaListenerContainerFactory<String, String> containerFactory) {
        return new KafkaSubscriberProcessor(eventCodec, properties, endpointRegistry, handlerMethodFactory, containerFactory);
    }

}
