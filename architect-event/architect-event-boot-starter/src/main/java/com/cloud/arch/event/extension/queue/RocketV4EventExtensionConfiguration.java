package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RocketmqProperties;
import com.cloud.arch.event.publisher.RocketEventPublisher;
import com.cloud.arch.event.subscriber.RocketmqSubscriberProcessor;
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
@ConditionalOnClass(name = "com.cloud.arch.event.props.RocketmqProperties")
@ConditionalOnProperty(prefix = "com.cloud.event.rocket.v4", name = "name-srv")
public class RocketV4EventExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.rocket.v4")
    public RocketmqProperties rocketmqProperties() {
        return new RocketmqProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(RocketmqProperties properties) {
        return new RocketEventPublisher(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public RocketmqSubscriberProcessor rocketmqSubscriberProcessor(EventCodec eventCodec,
                                                                   RocketmqProperties properties) {
        return new RocketmqSubscriberProcessor(eventCodec, properties);
    }

}
