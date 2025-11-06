package com.cloud.arch.event.extension.queue;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RocketmqV5Properties;
import com.cloud.arch.event.publisher.RocketmqV5EventPublisher;
import com.cloud.arch.event.subscriber.RocketmqV5SubscriberProcessor;
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
@ConditionalOnClass(name = "com.cloud.arch.event.props.RocketmqV5Properties")
@ConditionalOnProperty(prefix = "com.cloud.event.rocket.v5", name = "endpoints")
public class RocketV5EventExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.rocket.v5")
    public RocketmqV5Properties rocketmqV5Properties() {
        return new RocketmqV5Properties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public EventPublisher eventPublisher(RocketmqV5Properties properties) {
        return new RocketmqV5EventPublisher(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public RocketmqV5SubscriberProcessor rocketmqSubscriberProcessor(EventCodec eventCodec,
                                                                     RocketmqV5Properties properties) {
        return new RocketmqV5SubscriberProcessor(properties, eventCodec);
    }

}
