package com.cloud.token.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthProperty 授权属性")
class AuthPropertyTest {

    @Nested
    @DisplayName("payload()")
    class Payload {

        @Test
        @DisplayName("set → get 能取回")
        void shouldSetAndGetPayload() {
            AuthProperty prop = new AuthProperty();
            prop.payload("role", "admin");
            assertThat(prop.payload("role")).isEqualTo("admin");
        }

        @Test
        @DisplayName("多个 key → 各自独立")
        void shouldSupportMultipleKeys() {
            AuthProperty prop = new AuthProperty();
            prop.payload("role", "admin");
            prop.payload("tenant", "t001");
            assertThat(prop.payload("role")).isEqualTo("admin");
            assertThat(prop.payload("tenant")).isEqualTo("t001");
        }

        @Test
        @DisplayName("不存在 key → null")
        void shouldReturnNullForMissingKey() {
            AuthProperty prop = new AuthProperty();
            assertThat(prop.payload("missing")).isNull();
        }

        @Test
        @DisplayName("payload 为 null 时 get → null")
        void shouldReturnNullWhenPayloadIsNull() {
            AuthProperty prop = new AuthProperty();
            assertThat(prop.payload("any")).isNull();
        }
    }

    @Nested
    @DisplayName("hasPayload()")
    class HasPayload {

        @Test
        @DisplayName("有 payload → true")
        void shouldBeTrueWhenHasPayload() {
            AuthProperty prop = new AuthProperty();
            prop.payload("key", "value");
            assertThat(prop.hasPayload()).isTrue();
        }

        @Test
        @DisplayName("无 payload → false")
        void shouldBeFalseWithoutPayload() {
            AuthProperty prop = new AuthProperty();
            assertThat(prop.hasPayload()).isFalse();
        }

        @Test
        @DisplayName("空 payload → false")
        void shouldBeFalseWithEmptyPayload() {
            AuthProperty prop = new AuthProperty();
            prop.payload("k", "v");
            prop.payload("k", null); // 不改变 hasPayload
            assertThat(prop.hasPayload()).isTrue(); // map 非空
        }
    }

    @Nested
    @DisplayName("getDevice()")
    class GetDevice {

        @Test
        @DisplayName("设置了 device → 返回设置值")
        void shouldReturnSetDevice() {
            AuthProperty prop = new AuthProperty();
            prop.setDevice("android");
            assertThat(prop.getDevice()).isEqualTo("android");
        }

        @Test
        @DisplayName("未设置 device → 返回默认值")
        void shouldReturnDefaultDevice() {
            AuthProperty prop = new AuthProperty();
            assertThat(prop.getDevice()).isEqualTo("default");
        }

        @Test
        @DisplayName("device 为空字符串 → 返回默认值")
        void shouldReturnDefaultWhenEmpty() {
            AuthProperty prop = new AuthProperty();
            prop.setDevice("");
            assertThat(prop.getDevice()).isEqualTo("default");
        }

        @Test
        @DisplayName("device 为空白 → 返回默认值")
        void shouldReturnDefaultWhenBlank() {
            AuthProperty prop = new AuthProperty();
            prop.setDevice("  ");
            assertThat(prop.getDevice()).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("链式调用")
    class Chaining {

        @Test
        @DisplayName("setDevice 返回 this")
        void shouldSupportChaining() {
            AuthProperty prop = new AuthProperty()
                    .setDevice("ios")
                    .setTimeout(3600)
                    .setWriteHeader(true);
            assertThat(prop.getDevice()).isEqualTo("ios");
            assertThat(prop.getTimeout()).isEqualTo(3600);
            assertThat(prop.isWriteHeader()).isTrue();
        }
    }
}
