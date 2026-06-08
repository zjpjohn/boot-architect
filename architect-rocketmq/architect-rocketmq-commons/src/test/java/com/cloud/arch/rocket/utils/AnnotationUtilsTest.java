package com.cloud.arch.rocket.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnnotationUtils 注解工具")
class AnnotationUtilsTest {

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface A {}
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface B {}

    public void methodWithAnnotations(@A String a, @B int b) {}

    public void methodWithoutAnnotations(String a, int b) {}

    @Nested
    @DisplayName("annotationIndex()")
    class AnnotationIndex {

        @Test
        @DisplayName("找到注解 → 返回参数位置")
        void shouldFindAnnotationIndex() throws Exception {
            Method method = AnnotationUtilsTest.class.getDeclaredMethod("methodWithAnnotations", String.class, int.class);
            Annotation[][] anns = method.getParameterAnnotations();
            assertThat(AnnotationUtils.annotationIndex(anns, A.class)).isEqualTo(0);
            assertThat(AnnotationUtils.annotationIndex(anns, B.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("未找到注解 → 返回 null")
        void shouldReturnNullWhenNotFound() throws Exception {
            Method method = AnnotationUtilsTest.class.getDeclaredMethod("methodWithoutAnnotations", String.class, int.class);
            Annotation[][] anns = method.getParameterAnnotations();
            assertThat(AnnotationUtils.annotationIndex(anns, A.class)).isNull();
        }
    }
}
