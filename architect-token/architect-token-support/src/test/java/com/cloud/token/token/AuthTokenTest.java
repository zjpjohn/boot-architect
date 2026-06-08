package com.cloud.token.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthToken 授权 Token")
class AuthTokenTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            AuthToken token = new AuthToken();
            token.setToken("abc123");
            token.setRealm("default");
            token.setLoginId("user001");
            token.setTokenTtl(7200L);
            token.setSessionTtl(14400L);
            token.setDevice("web");
            token.setAttr("custom");

            assertThat(token.getToken()).isEqualTo("abc123");
            assertThat(token.getRealm()).isEqualTo("default");
            assertThat(token.getLoginId()).isEqualTo("user001");
            assertThat(token.getTokenTtl()).isEqualTo(7200L);
            assertThat(token.getSessionTtl()).isEqualTo(14400L);
            assertThat(token.getDevice()).isEqualTo("web");
            assertThat(token.getAttr()).isEqualTo("custom");
        }
    }
}
