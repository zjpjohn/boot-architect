package com.cloud.arch.event.boot;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.codec.FastJson2EventCodec;
import com.cloud.arch.event.commons.ApplicationContextHolder;
import com.cloud.arch.event.core.publish.EventMetadataFactory;
import com.cloud.arch.event.core.publish.MessageQueuePublisher;
import com.cloud.arch.event.props.PublishEventProperties;
import com.cloud.arch.event.publisher.EventPublisherSynchronization;
import com.cloud.arch.event.subscribe.EventSubscribeHandler;
import com.cloud.arch.event.subscribe.IdempotentChecker;
import com.cloud.arch.event.subscribe.IdempotentCleanScheduler;
import com.cloud.arch.event.subscribe.impl.TransactionIdempotentChecker;
import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.boot.CloudMutexAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 领域事件自动配置，按需装配发布端（事务同步器 + 消息队列发布器）和订阅端（幂等检查 + 清理调度）。
 */
@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableTransactionManagement
@AutoConfigureAfter(value = {DataSourceAutoConfiguration.class, CloudMutexAutoConfiguration.class})
public class CloudEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSourceTransactionManager.class)
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(EventCodec.class)
    public EventCodec eventCodec() {
        return new FastJson2EventCodec();
    }

    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    /**
     * 事件发布端配置：消息队列发布器、事务同步器、事件元数据工厂。
     */
    @Slf4j
    @Configuration
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @EnableConfigurationProperties(PublishEventProperties.class)
    @ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
    public static class EventPublisherConfiguration {

        @Bean
        public EventMetadataFactory eventMetadataFactory(EventCodec eventCodec) {
            return new EventMetadataFactory(eventCodec);
        }

        @Bean
        public MessageQueuePublisher queuePublisher(PublishEventProperties properties) {
            return new MessageQueuePublisher(properties.getPublisher().getPublishThreads(), properties.getPublisher()
                                                                                                      .getMaxPublishThreads(), properties.getPublisher()
                                                                                                                                         .getPublishCachedEventSize());
        }

        @Bean
        public EventPublisherSynchronization eventPublisherSynchronization(MessageQueuePublisher queuePublisher) {
            return new EventPublisherSynchronization(queuePublisher);
        }

    }

    /**
     * 事件订阅端配置：幂等检查器、事件处理器、幂等记录清理调度。
     */
    @Slf4j
    @Configuration
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @EnableConfigurationProperties(PublishEventProperties.class)
    @ConditionalOnProperty(prefix = "com.cloud.event.subscriber", name = "enable")
    public static class EventSubscriberConfiguration {

        @Bean
        public TransactionIdempotentChecker idempotentChecker(DataSourceTransactionManager transactionManager) {
            return new TransactionIdempotentChecker(transactionManager);
        }

        @Bean
        public EventSubscribeHandler eventSubscribeHandler(IdempotentChecker idempotentChecker) {
            return new EventSubscribeHandler(idempotentChecker);
        }

        @Bean
        public IdempotentCleanScheduler idempotentCleanScheduler(IdempotentChecker idempotentChecker,
                                                                 PublishEventProperties properties,
                                                                 MutexTemplate mutexTemplate) {
            return new IdempotentCleanScheduler(properties, mutexTemplate, idempotentChecker);
        }

    }

}
