package com.cloud.arch.web.support.metadata;

import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.annotation.Permission;
import com.cloud.arch.web.support.AuthorizationErrorHandler;
import com.cloud.arch.web.support.GrantAuthority;
import com.cloud.arch.web.utils.Assert;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

@Getter
public class AuthorizationMetadata {

    private final Class<?>            targetClass;
    private final Method              method;
    private final Permission          annotation;
    private final AnnotatedElementKey elementKey;
    private final Set<String>         domains;
    private final Set<String>         roles;
    private final Set<String>         permits;

    public AuthorizationMetadata(Class<?> targetClass, Method method, AnnotatedElementKey elementKey) {
        this.targetClass = targetClass;
        this.method      = method;
        this.elementKey  = elementKey;
        this.annotation  = this.findAuthAnnotation();
        this.domains     = this.valuesExtract(this.annotation.domain());
        this.roles       = this.valuesExtract(this.annotation.role());
        this.permits     = this.valuesExtract(this.annotation.permit());
    }

    private Set<String> valuesExtract(String[] source) {
        Set<String> values = Sets.newHashSet(source);
        if (values.contains(Permission.DEFAULT_VALUE)) {
            return Collections.emptySet();
        }
        return values;
    }

    /**
     * 元数据获取权限信息
     */
    public GrantAuthority requireAuthority(HttpServletRequest request) {
        String identity = request.getHeader(WebTokenConstants.AUTH_IDENTITY_HEADER);
        Assert.state(StringUtils.isNotBlank(identity), AuthorizationErrorHandler.AUTH_IDENTITY_NONE);
        return new GrantAuthority(identity, annotation.mode(), this.roles, this.permits);
    }

    private Permission findAuthAnnotation() {
        Permission permission = AnnotatedElementUtils.findMergedAnnotation(method, Permission.class);
        if (permission == null) {
            permission = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Permission.class);
        }
        if (permission == null) {
            permission = findAnnotationOnTarget(this.targetClass, method);
        }
        return permission;
    }

    private Permission findAnnotationOnTarget(Class<?> targetClass, Method method) {
        try {
            Method     targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
            Permission permission   = AnnotatedElementUtils.findMergedAnnotation(targetMethod, Permission.class);
            if (permission == null) {
                permission = AnnotatedElementUtils.findMergedAnnotation(targetMethod.getDeclaringClass(), Permission.class);
            }
            return permission;
        } catch (Exception e) {
            return null;
        }
    }

}
