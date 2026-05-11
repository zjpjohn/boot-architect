package com.cloud.arch.web.support;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.props.WebAuthorityProperties;
import com.cloud.arch.web.support.metadata.AuthorizationMetadata;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SecurityPrincipalProcessor implements ApplicationContextAware, SmartInitializingSingleton {

    private final Map<String, SecurityPrincipal> principalAuthorities = Maps.newHashMap();
    private final AuthorizeCacheManager          cacheManager;
    private       ApplicationContext             applicationContext;

    public SecurityPrincipalProcessor(AuthorizeCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 基于注解的权限校验
     */
    public boolean annotationAuthorize(AuthorizationMetadata metaData) {
        RequestAttributes  attributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request    = ((ServletRequestAttributes) attributes).getRequest();
        String             authDomain = request.getHeader(WebTokenConstants.ACCESS_SOURCE_HEADER);
        if (!metaData.isValidDomain(authDomain)) {
            return false;
        }
        GrantAuthority    authority         = metaData.requireAuthority(request);
        SecurityPrincipal principalSecurity = principalAuthorities.get(authDomain);
        if (principalSecurity == null || authority.isEmpty()) {
            //当系统未配置权限处理器或者权限/角色都为空直接返回true
            return true;
        }
        try {
            AuthorizeCacheKey cacheKey     = this.cacheKey(authDomain, authority.identity(), metaData);
            GrantedResult     cachedResult = this.cacheManager.fromCache(cacheKey);
            if (cachedResult != null) {
                return cachedResult.authorized();
            }
            GrantedResult result = authority.decide(principalSecurity);
            this.cacheManager.cacheAuthorize(cacheKey, result);
            return result.authorized();
        } catch (Exception error) {
            log.error("基于注解权限校验异常:", error);
        }
        return false;
    }

    /**
     * 对请求进行权限校验
     */
    public boolean uriAuthorize(UriResourceAuthority uriResource,
                                HandlerMethod handlerMethod,
                                String identity,
                                String authDomain) {
        if (!uriResource.isValidDomain(authDomain)) {
            return false;
        }
        SecurityPrincipal principalAuthority = principalAuthorities.get(authDomain);
        if (principalAuthority == null || uriResource.isEmptyRoleAndPermits()) {
            //当系统未配置对应域的权限处理器或者角色权限为空直接放行
            return true;
        }
        //从缓存中查找是否存在已校验的缓存结果
        AuthorizeCacheKey cacheKey     = this.cacheKey(authDomain, identity, handlerMethod);
        GrantedResult     cachedResult = this.cacheManager.fromCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult.authorized();
        }
        GrantAuthority grantAuthority  = uriResource.authority(identity);
        GrantedResult  authorityResult = grantAuthority.decide(principalAuthority);
        //缓存权限校验结果
        this.cacheManager.cacheAuthorize(cacheKey, authorityResult);
        return authorityResult.authorized();
    }

    private AuthorizeCacheKey cacheKey(String domain, String identity, AuthorizationMetadata metadata) {
        return new AuthorizeCacheKey(domain, identity, metadata.getElementKey());
    }

    private AuthorizeCacheKey cacheKey(String domain, String identity, HandlerMethod handlerMethod) {
        Object              candidate   = handlerMethod.getBean();
        Class<?>            targetClass = AopUtils.getTargetClass(candidate);
        Method              method      = AopUtils.getMostSpecificMethod(handlerMethod.getMethod(), targetClass);
        AnnotatedElementKey elementKey  = new AnnotatedElementKey(method, targetClass);
        return new AuthorizeCacheKey(domain, identity, elementKey);
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<SecurityPrincipal> authorities = this.getBeans(SecurityPrincipal.class);
        if (CollectionUtils.isNotEmpty(authorities)) {
            Map<String, SecurityPrincipal> authorityMap = Maps.uniqueIndex(authorities, SecurityPrincipal::domain);
            this.principalAuthorities.putAll(authorityMap);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public <T> List<T> getBeans(Class<? extends T> type) {
        try {
            return Lists.newArrayList(applicationContext.getBeansOfType(type).values());
        } catch (BeansException error) {
            if (log.isDebugEnabled()) {
                log.debug(error.getMessage(), error);
            }
        }
        return Collections.emptyList();
    }

}
