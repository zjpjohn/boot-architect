package com.cloud.arch.web.configurer;

import com.cloud.arch.web.advice.UniformResponseBodyAdvice;
import com.cloud.arch.web.advice.handler.GenericHandlerAdvice;
import com.cloud.arch.web.advice.handler.WebmvcHandlerAdvice;
import com.cloud.arch.web.converter.WebConverterConfigurer;
import com.cloud.arch.web.custom.CustomErrorAttributes;
import com.cloud.arch.web.custom.CustomWebMvcRegistrations;
import com.cloud.arch.web.custom.DictionaryEndpoint;
import com.cloud.arch.web.dict.DictionaryFactory;
import com.cloud.arch.web.props.WebmvcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = WebmvcProperties.PROPS_PREFIX, name = "enable", matchIfMissing = true)
@EnableConfigurationProperties({WebmvcProperties.class, WebMvcProperties.class})
public class WebmvcConfiguration {

    /**
     * 枚举、时间格式转换器
     */
    @Bean
    public WebConverterConfigurer webConverterConfigurer(WebMvcProperties properties) {
        return new WebConverterConfigurer(properties);
    }

    /**
     * 统一处理404异常和拦截器异常
     */
    @Bean
    public CustomErrorAttributes errorAttributes() {
        return new CustomErrorAttributes();
    }

    /**
     * 通用全局异常处理
     */
    @Bean
    @ConditionalOnMissingBean(GenericHandlerAdvice.class)
    public GenericHandlerAdvice genericHandlerAdvice() {
        return new GenericHandlerAdvice();
    }

    /**
     * 统一业务错误处理
     */
    @Bean
    @ConditionalOnMissingBean(WebmvcHandlerAdvice.class)
    public WebmvcHandlerAdvice webMvcHandlerAdvice() {
        return new WebmvcHandlerAdvice();
    }

    /**
     * Json响应体结构统一Advice
     */
    @Bean
    public UniformResponseBodyAdvice webResponseBodyAdvice(WebmvcProperties properties) {
        return new UniformResponseBodyAdvice(properties);
    }

    /**
     * 自定义MVC处理粘合器
     */
    @Bean
    public CustomWebMvcRegistrations customWebMvcRegistrations(UniformResponseBodyAdvice responseAdvice,
                                                               WebmvcProperties properties) {
        return new CustomWebMvcRegistrations(responseAdvice, properties);
    }

    @Configuration
    @ConditionalOnProperty(prefix = WebmvcProperties.PROPS_PREFIX + ".dictionary", name = "export")
    public static class DictionaryConfiguration {
        /**
         * 枚举值暴露字典数据
         */
        @Bean
        public DictionaryFactory dictionaryFactory() {
            return new DictionaryFactory();
        }

        /**
         * 字典端点配置
         */
        @Bean
        public DictionaryEndpoint dictionaryEndpoint(DictionaryFactory dictionaryFactory,
                                                     WebmvcProperties properties,
                                                     @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping requestMappingHandlerMapping) {
            return new DictionaryEndpoint(dictionaryFactory, properties, requestMappingHandlerMapping);
        }

    }

}
