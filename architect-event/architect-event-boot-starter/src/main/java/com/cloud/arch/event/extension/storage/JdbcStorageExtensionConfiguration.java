package com.cloud.arch.event.extension.storage;

import com.cloud.arch.event.JdbcCompensateEventScheduler;
import com.cloud.arch.event.JdbcCompensateProcessor;
import com.cloud.arch.event.JdbcCompensateProperties;
import com.cloud.arch.event.JdbcDomainEventRepository;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.mutex.MutexTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;


@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.cloud.arch.event.JdbcDomainEventRepository")
@ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
public class JdbcStorageExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.publisher.jdbc")
    public JdbcCompensateProperties compensateProperties() {
        return new JdbcCompensateProperties();
    }

    @Bean
    @Primary
    public IDomainEventRepository domainEventRepository(DataSource dataSource) {
        return new JdbcDomainEventRepository(dataSource);
    }

    @Bean
    public JdbcCompensateProcessor compensateProcessor(IDomainEventRepository eventRepository) {
        return new JdbcCompensateProcessor(eventRepository);
    }

    @Bean
    public JdbcCompensateEventScheduler compensateEventScheduler(MutexTemplate mutexTemplate,
                                                                 JdbcCompensateProperties properties,
                                                                 IDomainEventRepository eventRepository,
                                                                 JdbcCompensateProcessor compensateProcessor) {
        return new JdbcCompensateEventScheduler(mutexTemplate, properties, eventRepository, compensateProcessor);
    }

}
