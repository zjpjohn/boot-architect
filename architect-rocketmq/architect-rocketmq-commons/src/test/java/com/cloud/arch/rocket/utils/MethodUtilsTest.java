package com.cloud.arch.rocket.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MethodUtils 方法工具")
class MethodUtilsTest {

    @Nested
    @DisplayName("isDefault()")
    class IsDefault {

        @Test
        @DisplayName("接口中的 default 方法 → true")
        void shouldDetectDefaultMethod() throws Exception {
            Method method = InterfaceWithDefault.class.getMethod("defaultMethod");
            assertThat(MethodUtils.isDefault(method)).isTrue();
        }

        @Test
        @DisplayName("接口中的抽象方法 → false")
        void shouldNotDetectAbstractMethod() throws Exception {
            Method method = InterfaceWithDefault.class.getMethod("abstractMethod");
            assertThat(MethodUtils.isDefault(method)).isFalse();
        }

        @Test
        @DisplayName("类中的 public 方法 → false")
        void shouldNotDetectConcreteMethod() throws Exception {
            Method method = String.class.getMethod("length");
            assertThat(MethodUtils.isDefault(method)).isFalse();
        }
    }

    interface InterfaceWithDefault {
        void abstractMethod();

        default void defaultMethod() {
        }
    }
}
