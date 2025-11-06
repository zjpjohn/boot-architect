package com.cloud.arch.boot;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.cloud.arch.client.ElasticSearchProperties;
import com.cloud.arch.client.ElasticsearchClientFactoryBean;
import com.cloud.arch.repository.ElasticLogRepository;
import com.cloud.arch.repository.ILogQueryService;
import com.cloud.arch.repository.ILogRepository;
import com.cloud.arch.service.ElasticLogQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "com.cloud.logger.elastic", name = {"server", "index"})
public class LoggerElasticConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.logger.elastic")
    public ElasticSearchProperties elasticSearchProperties() {
        return new ElasticSearchProperties();
    }

    @Bean
    public ElasticsearchClientFactoryBean elasticsearchClient(ElasticSearchProperties properties) {
        return new ElasticsearchClientFactoryBean(properties);
    }

    @Bean
    public ILogRepository logRepository(ElasticSearchProperties properties, ElasticsearchClient elasticsearchClient) {
        return new ElasticLogRepository(properties, elasticsearchClient);
    }

    @Bean
    public ILogQueryService logQueryService(ILogRepository logRepository) {
        return new ElasticLogQueryService(logRepository);
    }
}
