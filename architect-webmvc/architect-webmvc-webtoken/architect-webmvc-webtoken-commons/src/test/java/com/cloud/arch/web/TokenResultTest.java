package com.cloud.arch.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenResult Token 结果")
class TokenResultTest {

    @Nested
    @DisplayName("record 字段")
    class RecordFields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            java.util.Date expire = new java.util.Date();
            TokenResult result = new TokenResult("token-abc", "tid-001", expire);
            assertThat(result.token()).isEqualTo("token-abc");
            assertThat(result.tokenId()).isEqualTo("tid-001");
            assertThat(result.expireAt()).isEqualTo(expire);
        }
    }
}
