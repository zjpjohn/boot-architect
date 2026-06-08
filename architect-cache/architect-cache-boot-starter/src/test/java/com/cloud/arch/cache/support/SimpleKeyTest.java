package com.cloud.arch.cache.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleKey 缓存键")
class SimpleKeyTest {

    @Nested
    @DisplayName("EMPTY 空键")
    class Empty {

        @Test
        @DisplayName("EMPTY 不为 null")
        void shouldNotBeNull() {
            assertThat(SimpleKey.EMPTY).isNotNull();
        }

        @Test
        @DisplayName("EMPTY 的 params 为空数组")
        void shouldHaveEmptyParams() {
            assertThat(SimpleKey.EMPTY.getParams()).isEmpty();
        }
    }

    @Nested
    @DisplayName("hashCode 与 equals")
    class Equality {

        @Test
        @DisplayName("相同参数生成的 key 相等")
        void shouldBeEqualWithSameParams() {
            SimpleKey k1 = new SimpleKey("a", 1);
            SimpleKey k2 = new SimpleKey("a", 1);
            assertThat(k1).isEqualTo(k2);
            assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
        }

        @Test
        @DisplayName("不同参数生成的 key 不相等")
        void shouldNotBeEqualWithDifferentParams() {
            SimpleKey k1 = new SimpleKey("a", 1);
            SimpleKey k2 = new SimpleKey("b", 1);
            assertThat(k1).isNotEqualTo(k2);
        }

        @Test
        @DisplayName("不同参数数量的 key 不相等")
        void shouldNotBeEqualWithDifferentParamCount() {
            SimpleKey k1 = new SimpleKey("a");
            SimpleKey k2 = new SimpleKey("a", "a");
            assertThat(k1).isNotEqualTo(k2);
        }

        @Test
        @DisplayName("两个 EMPTY 键相等")
        void shouldEqualTwoEmptyKeys() {
            SimpleKey k1 = new SimpleKey();
            SimpleKey k2 = SimpleKey.EMPTY;
            assertThat(k1).isEqualTo(k2);
            assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
        }

        @Test
        @DisplayName("数组参数使用 deep equality")
        void shouldUseDeepEqualityForArrays() {
            SimpleKey k1 = new SimpleKey(new Object[]{new Integer[]{1, 2}});
            SimpleKey k2 = new SimpleKey(new Object[]{new Integer[]{1, 2}});
            assertThat(k1).isEqualTo(k2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("包含类名")
        void shouldContainClassName() {
            assertThat(new SimpleKey("x").toString()).contains("SimpleKey");
        }
    }

    @Nested
    @DisplayName("参数保护性拷贝")
    class DefensiveCopy {

        @Test
        @DisplayName("修改原始数组不影响 SimpleKey 内部状态")
        void shouldNotBeAffectedByArrayMutation() {
            Object[] params = {"a", "b"};
            SimpleKey key = new SimpleKey(params);
            params[0] = "changed";

            assertThat(key.getParams()[0]).isEqualTo("a");
        }
    }
}
