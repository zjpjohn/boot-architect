package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.PulsarClientFactoryBean;
import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.PulsarMqProperties;
import com.cloud.arch.event.publisher.PulsarEventPublisher;
import com.cloud.arch.event.subscriber.PulsarSubscriberProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "com.cloud.arch.event.props.PulsarMqProperties")
@ConditionalOnProperty(prefix = "com.cloud.event.pulsar", name = "endpoints")
public class PulsarEventExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.pulsar")
    public PulsarMqProperties pulsarMqProperties() {
        return new PulsarMqProperties();
    }

    @Bean
    public PulsarClientFactoryBean pulsarClient(PulsarMqProperties properties) {
        return new PulsarClientFactoryBean(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(PulsarMqProperties properties, PulsarClient pulsarClient) {
        return new PulsarEventPublisher(pulsarClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public PulsarSubscriberProcessor subscriberProcessor(EventCodec eventCodec,
                                                         PulsarClient pulsarClient,
                                                         PulsarMqProperties properties) {
        return new PulsarSubscriberProcessor(properties, eventCodec, pulsarClient);
    }

}
