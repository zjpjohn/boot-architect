package com.cloud.arch.web.aspect;

import org.springframework.aop.support.annotation.AnnotationClassFilter;

import java.lang.annotation.Annotation;

public class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

    private final AnnotationMethodsResolver methodsResolver;

    public AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
        super(annotationType, true);
        this.methodsResolver = new AnnotationMethodsResolver(annotationType);
    }

    /**
     * 类标注目标注解或任一方法标注目标注解即匹配
     */
    @Override
    public boolean matches(Class<?> clazz) {
        //类标注@Permission或任一方法标注@Permission
        return super.matches(clazz) || this.methodsResolver.hasAnnotatedMethods(clazz);
    }

}
