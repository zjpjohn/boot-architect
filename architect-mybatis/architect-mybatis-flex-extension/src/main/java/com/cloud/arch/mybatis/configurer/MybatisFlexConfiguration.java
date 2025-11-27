package com.cloud.arch.mybatis.configurer;

import com.cloud.arch.mybatis.core.FlexGlobalConfigurer;
import com.cloud.arch.mybatis.core.TypeHandlerRegister;
import com.cloud.arch.mybatis.props.MybatisFlexProperties;
import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MybatisFlexProperties.class)
public class MybatisFlexConfiguration {

    /**
     * 全局注册枚举和JSON字段转换器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            TypeHandlerRegister.registry(registry);
        };
    }

    /**
     * 注册全局自定义主键生成器
     */
    @Bean
    public FlexGlobalConfigurer generatorConfig(MybatisFlexProperties properties) {
        return new FlexGlobalConfigurer(properties);
    }

}
