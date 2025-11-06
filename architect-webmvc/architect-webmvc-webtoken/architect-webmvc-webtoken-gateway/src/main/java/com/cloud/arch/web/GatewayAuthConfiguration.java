package com.cloud.arch.web;

import com.cloud.arch.web.impl.DefaultAuthSourceManager;
import com.cloud.arch.web.impl.DefaultBlackListValidator;
import com.cloud.arch.web.impl.JwtTokenVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.bootstrap.BootstrapApplicationListener")
public class GatewayAuthConfiguration {

    @Bean
    @ConfigurationProperties(prefix = WebTokenProperties.PROPS_PREFIX)
    public WebTokenProperties webTokenProperties() {
        return new WebTokenProperties();
    }

    /**
     * 扩展点-token黑名单校验失效token
     */
    @Bean
    @ConditionalOnMissingBean(ITokenBlackListValidator.class)
    public ITokenBlackListValidator tokenBlackListValidator() {
        return new DefaultBlackListValidator();
    }

    /**
     * 扩展点-请求域个性化字段处理域校验，与授权端扩展保持一致；必须自定义扩展否则所有请求将无法通过授权
     */
    @Bean
    @ConditionalOnMissingBean(IHttpAuthSourceManager.class)
    public IHttpAuthSourceManager httpAuthSourceManager() {
        return new DefaultAuthSourceManager();
    }

    /**
     * 扩展点-授权校验请求uri
     */
    @Bean
    @ConditionalOnMissingBean(IAuthRequestExclude.class)
    public IAuthRequestExclude authRequestExclude() {
        return new DefaultRequestExclude();
    }

    @Bean
    public ITokenVerifier tokenVerifier(WebTokenProperties properties,
                                        ITokenBlackListValidator tokenValidator,
                                        IHttpAuthSourceManager authSourceManager) {
        return new JwtTokenVerifier(properties, tokenValidator, authSourceManager);
    }

    @Bean
    public GatewayAuthFilter gatewayAuthFilter(ITokenVerifier tokenVerifier, IAuthRequestExclude authExcludeAccess) {
        return new GatewayAuthFilter(tokenVerifier, authExcludeAccess);
    }

}
