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

    /**
     * 判断指定类是否有方法标注了目标注解
     */
    public boolean hasAnnotatedMethods(Class<?> clazz) {
        final AtomicBoolean found = new AtomicBoolean(false);
        ReflectionUtils.doWithMethods(clazz, method -> {
            if (found.get()) {
                return;
            }
            Annotation annotation = AnnotationUtils.getAnnotation(method, annotationType);
            if (annotation != null) {
                found.set(true);
            }
        });
        return found.get();
    }
}
