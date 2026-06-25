package com.boot.architect.infrast.config;

import com.cloud.arch.executor.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExecutorConfiguration {


    @Bean
    public ExecutorFactory executorFactory() {
        return new ExecutorFactory();
    }

}
