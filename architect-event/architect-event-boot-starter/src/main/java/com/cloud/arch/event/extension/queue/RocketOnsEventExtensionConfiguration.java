package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.OnsQueueProperties;
import com.cloud.arch.event.publisher.OnsEventPublisher;
import com.cloud.arch.event.subscriber.OnsSubscriberProcessor;
import lombok.extern.slf4j.Slf4j;
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
@ConditionalOnClass(name = "com.cloud.arch.event.props.OnsQueueProperties")
@ConditionalOnProperty(prefix = "com.cloud.event.rocket.ons", name = {"access-key", "secret-key", "ons-address"})
public class RocketOnsEventExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.rocket.ons")
    public OnsQueueProperties onsQueueProperties() {
        return new OnsQueueProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(OnsQueueProperties properties) {
        return new OnsEventPublisher(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public OnsSubscriberProcessor onsSubscriberProcessor(EventCodec eventCodec, OnsQueueProperties properties) {
        return new OnsSubscriberProcessor(properties, eventCodec);
    }

}
