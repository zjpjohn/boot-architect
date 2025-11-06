package com.cloud.arch.web.configurer;

import com.cloud.arch.web.annotation.Permission;
import com.cloud.arch.web.aspect.AuthorizationAnnotationPointcutAdvisor;
import com.cloud.arch.web.interceptor.UriResourceAuthorizeInterceptor;
import com.cloud.arch.web.props.WebAuthorityProperties;
import com.cloud.arch.web.support.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(WebAuthorityProperties.class)
@ConditionalOnProperty(prefix = WebAuthorityProperties.PROPS_PREFIX, name = "enable", matchIfMissing = true)
public class WebAuthorityConfiguration {

    public static final String AUTHORIZATION_ADVISOR   = "authorization_pointcut_advisor";
    public static final String AUTHORIZE_CACHE_MANAGER = "authorize_cache_manager";

    @Bean(name = AUTHORIZE_CACHE_MANAGER)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AuthorizeCacheManager authorizeCacheManager(WebAuthorityProperties properties) {
        return new AuthorizeCacheManager(properties);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AnnotationSecurityHandler authorizationHandler(SecurityPrincipalProcessor principalSecurityProcessor) {
        return new AnnotationSecurityHandler(principalSecurityProcessor);
    }

    @Bean(name = AUTHORIZATION_ADVISOR)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AuthorizationAnnotationPointcutAdvisor authorizationAdvisor(AnnotationSecurityHandler authorizationHandler) {
        AuthorizationAnnotationPointcutAdvisor advisor = new AuthorizationAnnotationPointcutAdvisor(Permission.class);
        advisor.setAdvice(authorizationHandler);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SecurityPrincipalProcessor securityPrincipalProcessor() {
        return new SecurityPrincipalProcessor();
    }

    @Configuration
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnExpression("${com.cloud.web.security.enable:true}")
    public static class UriResourceAuthorityConfiguration {

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public UriAuthorityManager uriAuthorityManager(WebAuthorityProperties properties) {
            return new UriAuthorityManager(properties);
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public UriSecurityProcessor uriResourceProcessor(SecurityPrincipalProcessor securityPrincipalProcessor,
                                                         UriAuthorityManager uriAuthorityManager) {
            return new UriSecurityProcessor(securityPrincipalProcessor, uriAuthorityManager);
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public UriResourceAuthorizeInterceptor authorizeInterceptor(UriSecurityProcessor uriSecurityProcessor) {
            return new UriResourceAuthorizeInterceptor(uriSecurityProcessor);
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public AuthorityInterceptorConfigurer interceptorConfigurer(UriAuthorityManager uriAuthorityManager,
                                                                    UriResourceAuthorizeInterceptor authorizeInterceptor) {
            return new AuthorityInterceptorConfigurer(uriAuthorityManager, authorizeInterceptor);
        }
    }
}
