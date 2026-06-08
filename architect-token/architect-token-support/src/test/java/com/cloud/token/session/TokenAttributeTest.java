package com.cloud.token.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("TokenAttribute Token 属性")
class TokenAttributeTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("全参构造正确赋值")
        void shouldSetAllFields() {
            TokenAttribute attr = new TokenAttribute("token123", "android", "extra");
            assertThat(attr.getToken()).isEqualTo("token123");
            assertThat(attr.getDevice()).isEqualTo("android");
            assertThat(attr.getAttr()).isEqualTo("extra");
        }

        @Test
        @DisplayName("双参构造不设置 attr")
        void shouldNotSetAttrWithTwoArgConstructor() {
            TokenAttribute attr = new TokenAttribute("token123", "ios");
            assertThat(attr.getToken()).isEqualTo("token123");
            assertThat(attr.getDevice()).isEqualTo("ios");
            assertThat(attr.getAttr()).isNull();
        }
    }

    @Nested
    @DisplayName("equals() / hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同 token → equals true")
        void shouldBeEqualWithSameToken() {
            TokenAttribute a1 = new TokenAttribute("t1", "android");
            TokenAttribute a2 = new TokenAttribute("t1", "ios");
            assertThat(a1).isEqualTo(a2);
        }

        @Test
        @DisplayName("不同 token → equals false")
        void shouldNotBeEqualWithDifferentToken() {
            TokenAttribute a1 = new TokenAttribute("t1", "android");
            TokenAttribute a2 = new TokenAttribute("t2", "android");
            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        @DisplayName("相同 token → hashCode 相同")
        void shouldHaveSameHashCodeWithSameToken() {
            TokenAttribute a1 = new TokenAttribute("t1", "android");
            TokenAttribute a2 = new TokenAttribute("t1", "ios", "other");
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }
    }
}
