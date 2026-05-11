package com.cloud.arch.web.support;

import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.annotation.Permission;
import com.cloud.arch.web.utils.Assert;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

@Slf4j
public class UriSecurityProcessor {

    private final SecurityPrincipalProcessor securityProcessor;
    private final UriAuthorityManager        uriAuthorityManager;

    public UriSecurityProcessor(SecurityPrincipalProcessor securityProcessor, UriAuthorityManager uriAuthorityManager) {
        this.securityProcessor = securityProcessor;
        this.uriAuthorityManager = uriAuthorityManager;
    }

    /**
     * 判断是否标注 Permission 注解，有注解存在方法不进行拦截
     */
    public boolean isAuthAnnotated(HandlerMethod target) {
        return target.hasMethodAnnotation(Permission.class) ||
               target.getBeanType().getAnnotation(Permission.class) != null;
    }

    /**
     * 请求方法权限校验
     */
    public boolean authorize(HttpServletRequest request, HandlerMethod handlerMethod) {
        UriResourceAuthority uriResource = uriAuthorityManager.measureAuthority(request, handlerMethod);
        if (uriResource == null) {
            return true;
        }
        String authDomain = request.getHeader(WebTokenConstants.ACCESS_SOURCE_HEADER);
        String identity   = request.getHeader(WebTokenConstants.AUTH_IDENTITY_HEADER);
        AuthorizationErrorHandler.CHANNEL_NULL.check(authDomain);
        AuthorizationErrorHandler.AUTH_IDENTITY_NONE.check(identity);
        return securityProcessor.uriAuthorize(uriResource, handlerMethod, identity, authDomain);
    }

}
