package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RabbitmqProperties;
import com.cloud.arch.event.publisher.RabbitEventPublisher;
import com.cloud.arch.event.subscriber.RabbitSubscriberProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "com.cloud.arch.event.props.RabbitmqProperties")
@AutoConfigureAfter(RabbitAutoConfiguration.class)
public class RabbitEventExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.rabbit")
    public RabbitmqProperties rabbitmqProperties() {
        return new RabbitmqProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(ConnectionFactory connectionFactory, RabbitmqProperties properties) {
        return new RabbitEventPublisher(properties, connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public RabbitSubscriberProcessor rabbitSubscriberProcessor(AmqpAdmin amqpAdmin,
                                                               RabbitListenerEndpointRegistry endpointRegistry,
                                                               DirectRabbitListenerContainerFactory containerFactory,
                                                               RabbitmqProperties properties,
                                                               EventCodec eventCodec) {
        return new RabbitSubscriberProcessor(eventCodec, properties, amqpAdmin, containerFactory, endpointRegistry);
    }

}
