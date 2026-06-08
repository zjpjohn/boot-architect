package com.cloud.arch.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CodecTypeUtils 编解码类型工具")
class CodecTypeUtilsTest {

    @Nested
    @DisplayName("私有构造")
    class PrivateConstructor {

        @Test
        @DisplayName("反射调用抛 UnsupportedOperationException")
        void shouldThrowWhenInstantiated() throws Exception {
            java.lang.reflect.Constructor<CodecTypeUtils> ctor = CodecTypeUtils.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            assertThatThrownBy(ctor::newInstance)
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("typList()")
    class TypList {

        @Test
        @DisplayName("返回 Class 数组")
        void shouldReturnClassArray() {
            Class<?>[] types = CodecTypeUtils.typList();
            assertThat(types).isNotNull();
            assertThat(types).isInstanceOf(Class[].class);
        }
    }
}
