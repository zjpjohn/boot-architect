package com.cloud.arch.web.aspect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
public class AuthorizationAnnotationPointcutAdvisor extends StaticMethodMatcherPointcutAdvisor {

    private final MethodMatcher         methodResolver;
    private final AnnotationClassFilter annotationClassFilter;

    public AuthorizationAnnotationPointcutAdvisor(Class<? extends Annotation> annotationType) {
        this.methodResolver = new AnnotationMethodMatcher(annotationType);
        // 在方法第一次调用时判断类上是否有@Permission注解
        this.annotationClassFilter = new AnnotationClassFilter(annotationType, true);
        // 项目启动时调用ClassFilter扫描一次满足条件的权限注解类,缩小后续匹配范围
        // 类上有@Permission注解或者类的任一方法上有@Permission注解
        setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        //类上有@Permission注解的所有方法或者方法上有@Permission注解，将满足权限过滤条件
        return this.annotationClassFilter.matches(targetClass) || this.methodResolver.matches(method, targetClass);
    }

}
