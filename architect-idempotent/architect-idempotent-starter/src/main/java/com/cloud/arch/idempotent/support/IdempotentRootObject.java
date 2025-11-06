package com.cloud.arch.idempotent.support;

import com.cloud.arch.idempotent.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public record IdempotentRootObject(Method method, Object[] args, Object target, Class<?> targetClass) {

    public Method getTargetMethod() {
        if (Proxy.isProxyClass(targetClass)) {
            return AopUtils.getMostSpecificMethod(method, targetClass);
        }
        return method;
    }

    public Idempotent getAnnotation() {
        return method.getAnnotation(Idempotent.class);
    }

    public static IdempotentRootObject of(ProceedingJoinPoint joinPoint) {
        Object   value       = joinPoint.getTarget();
        Object[] pointArgs   = joinPoint.getArgs();
        Class<?> targetClazz = AopProxyUtils.ultimateTargetClass(value);
        Method   method      = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return new IdempotentRootObject(method, pointArgs, value, targetClazz);
    }

}
