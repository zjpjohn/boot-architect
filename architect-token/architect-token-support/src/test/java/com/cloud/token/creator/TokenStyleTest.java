package com.cloud.token.creator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenStyle Token 生成风格")
class TokenStyleTest {

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("\"uuid\" → STYLE_UUID")
        void shouldMapUuid() {
            assertThat(TokenStyle.of("uuid")).isEqualTo(TokenStyle.STYLE_UUID);
        }

        @Test
        @DisplayName("\"simple-uuid\" → STYLE_SIMPLE_UUID")
        void shouldMapSimpleUuid() {
            assertThat(TokenStyle.of("simple-uuid")).isEqualTo(TokenStyle.STYLE_SIMPLE_UUID);
        }

        @Test
        @DisplayName("\"random-32\" → STYLE_RANDOM_32")
        void shouldMapRandom32() {
            assertThat(TokenStyle.of("random-32")).isEqualTo(TokenStyle.STYLE_RANDOM_32);
        }

        @Test
        @DisplayName("\"random-64\" → STYLE_RANDOM_64")
        void shouldMapRandom64() {
            assertThat(TokenStyle.of("random-64")).isEqualTo(TokenStyle.STYLE_RANDOM_64);
        }

        @Test
        @DisplayName("\"random-128\" → STYLE_RANDOM_128")
        void shouldMapRandom128() {
            assertThat(TokenStyle.of("random-128")).isEqualTo(TokenStyle.STYLE_RANDOM_128);
        }

        @Test
        @DisplayName("未知值 → 默认 STYLE_UUID")
        void shouldDefaultToUuidForUnknown() {
            assertThat(TokenStyle.of("unknown")).isEqualTo(TokenStyle.STYLE_UUID);
        }

        @Test
        @DisplayName("null → 默认 STYLE_UUID")
        void shouldDefaultToUuidForNull() {
            assertThat(TokenStyle.of(null)).isEqualTo(TokenStyle.STYLE_UUID);
        }
    }

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("STYLE_UUID → 36 位 UUID 格式")
        void shouldGenerateUuidFormat() {
            String token = TokenStyle.STYLE_UUID.generate();
            assertThat(token).hasSize(36);
            assertThat(token.split("-")).hasSize(5);
        }

        @Test
        @DisplayName("STYLE_SIMPLE_UUID → 32 位无横线")
        void shouldGenerateSimpleUuidFormat() {
            String token = TokenStyle.STYLE_SIMPLE_UUID.generate();
            assertThat(token).hasSize(32);
            assertThat(token).doesNotContain("-");
        }

        @Test
        @DisplayName("STYLE_RANDOM_32 → 32 位随机字符串")
        void shouldGenerateRandom32() {
            String token = TokenStyle.STYLE_RANDOM_32.generate();
            assertThat(token).hasSize(32);
        }

        @Test
        @DisplayName("STYLE_RANDOM_64 → 64 位随机字符串")
        void shouldGenerateRandom64() {
            String token = TokenStyle.STYLE_RANDOM_64.generate();
            assertThat(token).hasSize(64);
        }

        @Test
        @DisplayName("STYLE_RANDOM_128 → 128 位随机字符串")
        void shouldGenerateRandom128() {
            String token = TokenStyle.STYLE_RANDOM_128.generate();
            assertThat(token).hasSize(128);
        }

        @Test
        @DisplayName("每次生成不同值")
        void shouldGenerateUniqueValues() {
            String t1 = TokenStyle.STYLE_UUID.generate();
            String t2 = TokenStyle.STYLE_UUID.generate();
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    @Nested
    @DisplayName("randomString()")
    class RandomString {

        @Test
        @DisplayName("生成指定长度字符串")
        void shouldGenerateSpecifiedLength() {
            assertThat(TokenStyle.randomString(10)).hasSize(10);
            assertThat(TokenStyle.randomString(20)).hasSize(20);
            assertThat(TokenStyle.randomString(0)).hasSize(0);
        }

        @Test
        @DisplayName("只包含合法字符集")
        void shouldOnlyContainValidChars() {
            String result = TokenStyle.randomString(100);
            assertThat(result).matches("[a-zA-Z0-9]+");
        }
    }

    @Nested
    @DisplayName("getName()")
    class GetName {

        @Test
        @DisplayName("返回 name 字段值")
        void shouldReturnName() {
            assertThat(TokenStyle.STYLE_UUID.getName()).isEqualTo("uuid");
            assertThat(TokenStyle.STYLE_RANDOM_32.getName()).isEqualTo("random-32");
        }
    }
}
