package com.cloud.arch.web.support.metadata;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class AuthorizationMetadataFactory {

    //权限校验方法元数据缓存
    private final Map<AnnotatedElementKey, AuthorizationMetadata> metaDataCache = Maps.newConcurrentMap();

    /**
     * 获取并创建授权元数据
     */
    public AuthorizationMetadata getAndCreate(MethodInvocation invocation) {
        Object              candidate   = Objects.requireNonNull(invocation.getThis());
        Class<?>            targetClass = AopUtils.getTargetClass(candidate);
        Method              method      = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        AnnotatedElementKey elementKey  = new AnnotatedElementKey(method, targetClass);
        return metaDataCache.computeIfAbsent(elementKey,
                key -> new AuthorizationMetadata(targetClass, method, elementKey));
    }

}
