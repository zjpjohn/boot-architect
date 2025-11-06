package com.cloud.arch.mybatis.configurer;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cloud.arch.mybatis.core.CustomIdGenerator;
import com.cloud.arch.mybatis.core.TimeMetaObjectHandler;
import com.cloud.arch.mybatis.core.TypeHandlerRegister;
import com.cloud.arch.mybatis.props.MybatisPlusProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfiguration {


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
     * 自定义主键生成器
     */
    @Bean
    @ConditionalOnProperty(prefix = MybatisPlusProperties.PROPS_PREFIX, name = "enable-id", havingValue = "true")
    public IdentifierGenerator identifierGenerator() {
        return new CustomIdGenerator();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //启用乐观锁
        if (properties.getVersion().isEnable()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }
        //启用分页查询
        if (properties.getPagination().isEnable()) {
            Long maxLimit = properties.getPagination().getMaxLimit();
            if (maxLimit <= 0 || maxLimit > 1000) {
                throw new IllegalArgumentException("分页查询数量错误，该参数仅允许(0,1000].");
            }
            PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
            paginationInnerInterceptor.setDbType(properties.getPagination().getType());
            paginationInnerInterceptor.setMaxLimit(maxLimit);
            interceptor.addInnerInterceptor(paginationInnerInterceptor);
        }
        return interceptor;
    }

    /**
     * 创建/更新时间字段自动填充
     */
    @Bean
    public MetaObjectHandler metaObjectHandler(MybatisPlusProperties properties) {
        return new TimeMetaObjectHandler(properties);
    }

}
