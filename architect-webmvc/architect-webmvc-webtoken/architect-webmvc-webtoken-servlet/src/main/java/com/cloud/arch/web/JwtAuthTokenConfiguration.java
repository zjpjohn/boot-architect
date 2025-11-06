package com.cloud.arch.web;

import com.cloud.arch.web.filter.JwtAuthTokenFilter;
import com.cloud.arch.web.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties({TokenAuthProperties.class})
@ConditionalOnProperty(prefix = TokenAuthProperties.PROPS_PREFIX, name = "enable", matchIfMissing = true)
public class JwtAuthTokenConfiguration {

    @Bean
    @ConfigurationProperties(prefix = WebTokenProperties.PROPS_PREFIX)
    public WebTokenProperties webTokenProperties() {
        return new WebTokenProperties();
    }

    /**
     * token黑名单失效事件发布器
     */
    @Bean
    @ConditionalOnMissingBean(ITokenBlackListPublisher.class)
    public ITokenBlackListPublisher tokenBlackListPublisher() {
        return new DefaultBlackListPublisher();
    }

    @Bean
    public ITokenCreator tokenCreator(WebTokenProperties properties) {
        return new JwtTokenCreator(properties);
    }

    /**
     * JwtAuthTokenFilter仅在springboot环境下启用， spring cloud环境下请在gateway端进行token校验
     */
    @Slf4j
    @Configuration
    @ConditionalOnProperty(prefix = TokenAuthProperties.PROPS_PREFIX, name = "enable", matchIfMissing = true)
    @ConditionalOnMissingClass(value = "org.springframework.cloud.bootstrap.BootstrapApplicationListener")
    public static class WebBootAuthConfiguration {

        /**
         * 扩展点-token黑名单校验，与token黑名单发布器扩展实现应一致
         */
        @Bean
        @ConditionalOnMissingBean(ITokenBlackListValidator.class)
        public ITokenBlackListValidator tokenBlackListValidator() {
            return new DefaultBlackListValidator();
        }

        /**
         * 可自定义扩展-请求授权域个性化字段处理，与鉴权端保持一致(主要是场景-网关应用与授权应用分离时必须保持一致)
         */
        @Bean
        @ConditionalOnMissingBean(IHttpAuthSourceManager.class)
        public IHttpAuthSourceManager httpAuthSourceManager() {
            return new DefaultAuthSourceManager();
        }

        @Bean
        public ITokenVerifier tokenVerifier(WebTokenProperties properties,
                                            ITokenBlackListValidator tokenValidator,
                                            IHttpAuthSourceManager authSourceManager) {
            return new JwtTokenVerifier(properties, tokenValidator, authSourceManager);
        }

        @Bean
        public JwtAuthTokenFilter jwtAuthTokenFilter(TokenAuthProperties properties, ITokenVerifier tokenVerifier) {
            return new JwtAuthTokenFilter(tokenVerifier, properties);
        }

        @Bean
        public FilterRegistrationBean<JwtAuthTokenFilter> jwtAuthFilterRegistration(JwtAuthTokenFilter jwtAuthTokenFilter,
                                                                                    TokenAuthProperties properties) {
            FilterRegistrationBean<JwtAuthTokenFilter> registration = new FilterRegistrationBean<>();
            registration.setOrder(Ordered.LOWEST_PRECEDENCE);
            registration.setFilter(jwtAuthTokenFilter);
            registration.setName(JwtAuthTokenFilter.AUTH_FILTER_NAME);
            registration.addUrlPatterns(properties.patterns().toArray(new String[0]));
            return registration;
        }
    }

}
