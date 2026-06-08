package com.cloud.token.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenConstants Token 常量")
class TokenConstantsTest {

    @Nested
    @DisplayName("前缀常量")
    class Prefixes {

        @Test
        @DisplayName("TOKEN_PREFIX = \"t\"")
        void shouldHaveCorrectTokenPrefix() {
            assertThat(TokenConstants.TOKEN_PREFIX).isEqualTo("t");
        }

        @Test
        @DisplayName("SESSION_PREFIX = \"s\"")
        void shouldHaveCorrectSessionPrefix() {
            assertThat(TokenConstants.SESSION_PREFIX).isEqualTo("s");
        }
    }

    @Nested
    @DisplayName("过期常量")
    class ExpireValues {

        @Test
        @DisplayName("NEVER_EXPIRE = -1")
        void shouldHaveNeverExpire() {
            assertThat(TokenConstants.NEVER_EXPIRE).isEqualTo(-1L);
        }

        @Test
        @DisplayName("NO_EXPIRE_VALUE = -2")
        void shouldHaveNoExpireValue() {
            assertThat(TokenConstants.NO_EXPIRE_VALUE).isEqualTo(-2L);
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("DEFAULT_DEVICE = \"default\"")
        void shouldHaveDefaultDevice() {
            assertThat(TokenConstants.DEFAULT_DEVICE).isEqualTo("default");
        }

        @Test
        @DisplayName("DEFAULT_REALM = \"default\"")
        void shouldHaveDefaultRealm() {
            assertThat(TokenConstants.DEFAULT_REALM).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("禁言常量")
    class MutedValues {

        @Test
        @DisplayName("DEFAULT_MUTED_LEVEL = 1")
        void shouldHaveDefaultMutedLevel() {
            assertThat(TokenConstants.DEFAULT_MUTED_LEVEL).isEqualTo(1);
        }

        @Test
        @DisplayName("MIN_MUTED_LEVEL = 1")
        void shouldHaveMinMutedLevel() {
            assertThat(TokenConstants.MIN_MUTED_LEVEL).isEqualTo(1);
        }

        @Test
        @DisplayName("NO_MUTED_LEVEL = -2")
        void shouldHaveNoMutedLevel() {
            assertThat(TokenConstants.NO_MUTED_LEVEL).isEqualTo(-2);
        }

        @Test
        @DisplayName("AUTH_SAFE_VALUE = \"safe\"")
        void shouldHaveAuthSafeValue() {
            assertThat(TokenConstants.AUTH_SAFE_VALUE).isEqualTo("safe");
        }
    }
}
