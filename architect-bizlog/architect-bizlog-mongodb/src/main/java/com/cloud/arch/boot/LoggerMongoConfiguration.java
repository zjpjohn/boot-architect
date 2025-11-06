package com.cloud.arch.boot;

import com.cloud.arch.repository.ILogQueryService;
import com.cloud.arch.repository.ILogRepository;
import com.cloud.arch.repository.MongoLogRepository;
import com.cloud.arch.service.MongoLogQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class LoggerMongoConfiguration {

    @Bean
    public ILogRepository logRepository(MongoTemplate mongoTemplate) {
        return new MongoLogRepository(mongoTemplate);
    }

    @Bean
    public ILogQueryService logQueryService(ILogRepository logRepository) {
        return new MongoLogQueryService(logRepository);
    }

}
