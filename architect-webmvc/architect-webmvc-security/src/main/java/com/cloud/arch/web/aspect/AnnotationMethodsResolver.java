package com.cloud.arch.web.aspect;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnnotationMethodsResolver {

    private final Class<? extends Annotation> annotationType;

    public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public boolean hasAnnotatedMethods(Class<?> clazz) {
        final AtomicBoolean founded = new AtomicBoolean(false);
        ReflectionUtils.doWithMethods(clazz, method -> {
            if (founded.get()) {
                return;
            }
            Annotation annotation = AnnotationUtils.getAnnotation(method, annotationType);
            if (annotation != null) {
                founded.set(true);
            }
        });
        return founded.get();
    }
}
