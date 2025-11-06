package com.cloud.arch.mybatis;

import com.cloud.arch.mybatis.core.TypeHandlerRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MybatisExtensionConfiguration {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            try {
                TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
                TypeHandlerRegister.registry(registry);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
