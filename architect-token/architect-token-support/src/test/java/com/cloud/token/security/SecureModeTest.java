package com.cloud.token.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecureMode 权限校验模式")
class SecureModeTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValues {

        @Test
        @DisplayName("AND → 所有条件满足")
        void shouldHaveAnd() {
            assertThat(SecureMode.valueOf("AND")).isEqualTo(SecureMode.AND);
        }

        @Test
        @DisplayName("OR → 至少其一满足")
        void shouldHaveOr() {
            assertThat(SecureMode.valueOf("OR")).isEqualTo(SecureMode.OR);
        }

        @Test
        @DisplayName("两个枚举值")
        void shouldHaveTwoValues() {
            assertThat(SecureMode.values()).hasSize(2);
        }
    }
}
