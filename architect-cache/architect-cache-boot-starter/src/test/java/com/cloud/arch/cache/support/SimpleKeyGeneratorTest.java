package com.cloud.arch.cache.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleKeyGenerator 默认缓存键生成器")
class SimpleKeyGeneratorTest {

    private SimpleKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SimpleKeyGenerator();
    }

    @Nested
    @DisplayName("无参场景")
    class NoParams {

        @Test
        @DisplayName("null 参数返回 EMPTY")
        void shouldReturnEmptyForNullParams() {
            Object key = generator.generate(null, null);
            assertThat(key).isSameAs(SimpleKey.EMPTY);
        }

        @Test
        @DisplayName("空数组返回 EMPTY")
        void shouldReturnEmptyForEmptyArray() {
            Object key = generator.generate(null, null);
            assertThat(key).isSameAs(SimpleKey.EMPTY);
        }
    }

    @Nested
    @DisplayName("单参场景")
    class SingleParam {

        @Test
        @DisplayName("单个非数组参数直接返回参数本身")
        void shouldReturnParamDirectly() {
            Object key = generator.generate(null, null, "hello");
            assertThat(key).isEqualTo("hello");
        }

        @Test
        @DisplayName("单参数为 null 时返回 SimpleKey 包装（null 不满足直接返回条件）")
        void shouldReturnSimpleKeyForNullParam() {
            Object key = generator.generate(null, null, (Object) null);
            assertThat(key).isInstanceOf(SimpleKey.class);
        }

        @Test
        @DisplayName("单参数为数组时返回 SimpleKey")
        void shouldReturnSimpleKeyForArrayParam() {
            Object key = generator.generate(null, null, new int[]{1, 2});
            assertThat(key).isInstanceOf(SimpleKey.class);
        }
    }

    @Nested
    @DisplayName("多参场景")
    class MultiParams {

        @Test
        @DisplayName("多个参数返回 SimpleKey")
        void shouldReturnSimpleKeyForMultipleParams() {
            Object key = generator.generate(null, null, "a", 1, true);
            assertThat(key).isInstanceOf(SimpleKey.class);
        }

        @Test
        @DisplayName("相同多参生成的 SimpleKey 相等")
        void shouldGenerateEqualKeysForSameParams() {
            Object k1 = generator.generate(null, null, "x", 42);
            Object k2 = generator.generate(null, null, "x", 42);
            assertThat(k1).isEqualTo(k2);
        }

        @Test
        @DisplayName("不同多参生成的 SimpleKey 不相等")
        void shouldGenerateDifferentKeysForDifferentParams() {
            Object k1 = generator.generate(null, null, "x", 42);
            Object k2 = generator.generate(null, null, "y", 42);
            assertThat(k1).isNotEqualTo(k2);
        }
    }
}
