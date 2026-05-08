package com.cloud.arch.web.support.metadata;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.annotation.Permission;
import com.cloud.arch.web.support.AuthorizationErrorHandler;
import com.cloud.arch.web.support.GrantAuthority;
import com.cloud.arch.web.support.GrantMode;
import com.cloud.arch.web.utils.Assert;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Getter
public class AuthorizationMetadata {

    private final Class<?>            targetClass;
    private final Method              method;
    private final Permission          methodAnnotation;
    private final Permission          targetAnnotation;
    private final AnnotatedElementKey elementKey;
    private final PermitAuthority     authority;

    public AuthorizationMetadata(Class<?> targetClass, Method method, AnnotatedElementKey elementKey) {
        this.targetClass = targetClass;
        this.method = method;
        this.elementKey = elementKey;
        this.methodAnnotation = AnnotatedElementUtils.getMergedAnnotation(method, Permission.class);
        this.targetAnnotation = AnnotatedElementUtils.getMergedAnnotation(targetClass, Permission.class);
        this.authority = new PermitAuthority(this.methodAnnotation, this.targetAnnotation);
    }

    public boolean isEmptyAuthorization() {
        return this.targetAnnotation == null && this.methodAnnotation == null;
    }

    /**
     * 判断目标访问域是否有效
     */
    public boolean isValidDomain(String target) {
        return StringUtils.isNotBlank(target) &&
               (CollectionUtils.isEmpty(this.authority.domains) || this.authority.domains.contains(target));
    }

    /**
     * 构造角色权限元数据信息
     */
    public GrantAuthority requireAuthority(HttpServletRequest request) {
        String identity = request.getHeader(WebTokenConstants.AUTH_IDENTITY_HEADER);
        Assert.state(StringUtils.isNotBlank(identity), AuthorizationErrorHandler.AUTH_IDENTITY_NONE);
        return new GrantAuthority(identity, authority.mode, this.authority.roles, this.authority.permits);
    }

    public static class PermitAuthority {

        private final Set<String> domains = Sets.newHashSet();
        private final Set<String> roles   = Sets.newHashSet();
        private final Set<String> permits = Sets.newHashSet();
        private       GrantMode   mode    = GrantMode.AND;

        public PermitAuthority(Permission methodAnnotation, Permission targetAnnotation) {
            this.extractPermitsAndRoles(methodAnnotation, targetAnnotation);
            this.extractDomains(methodAnnotation, targetAnnotation);
        }

        /**
         * 请求访问域类上配置和方法配置合并
         * 1.类上配置访问域方法未配置，使用类配置的访问域，简化同一类访问域配置
         * 2.类上未配置方法配置，使用方法配置的访问域，精细化配置请求访问域
         * 3.二者都配置访问域合并访问域
         */
        private void extractDomains(Permission methodAnnotation, Permission targetAnnotation) {
            Optional.ofNullable(methodAnnotation)
                    .map(v -> valuesExtract(v.domain()))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(domains::addAll);
            Optional.ofNullable(targetAnnotation)
                    .map(v -> valuesExtract(v.domain()))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(domains::addAll);
        }

        /**
         * 角色权限方法覆盖类上配置
         * 方法权限角色配置优先级高于类上配置的角色权限
         */
        private void extractPermitsAndRoles(Permission methodAnnotation, Permission targetAnnotation) {
            if (methodAnnotation != null) {
                this.roles.addAll(valuesExtract(methodAnnotation.role()));
                this.permits.addAll(valuesExtract(methodAnnotation.permit()));
                this.mode = methodAnnotation.mode();
            } else if (targetAnnotation != null) {
                this.roles.addAll(valuesExtract(targetAnnotation.role()));
                this.permits.addAll(valuesExtract(targetAnnotation.permit()));
                this.mode = targetAnnotation.mode();
            }
        }

        private Set<String> valuesExtract(String[] source) {
            Set<String> values = Sets.newHashSet(source);
            if (values.contains(Permission.DEFAULT_VALUE)) {
                return Collections.emptySet();
            }
            return values;
        }

    }
}
