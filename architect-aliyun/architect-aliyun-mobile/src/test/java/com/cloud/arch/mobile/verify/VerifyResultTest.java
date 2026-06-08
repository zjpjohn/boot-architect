package com.cloud.arch.mobile.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VerifyResult 认证结果")
class VerifyResultTest {

    @Nested
    @DisplayName("of() 转换")
    class Of {

        @Test
        @DisplayName("\"PASS\" → PASS")
        void shouldMapPass() {
            assertThat(VerifyResult.of("PASS")).isEqualTo(VerifyResult.PASS);
        }

        @Test
        @DisplayName("\"REJECT\" → REJECT")
        void shouldMapReject() {
            assertThat(VerifyResult.of("REJECT")).isEqualTo(VerifyResult.REJECT);
        }

        @Test
        @DisplayName("\"UNKNOWN\" → UNKNOWN")
        void shouldMapUnknown() {
            assertThat(VerifyResult.of("UNKNOWN")).isEqualTo(VerifyResult.UNKNOWN);
        }

        @Test
        @DisplayName("未知值 → 抛出异常")
        void shouldThrowForUnknown() {
            assertThatThrownBy(() -> VerifyResult.of("INVALID"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("未知的认证结果");
        }
    }

    @Nested
    @DisplayName("Value 接口")
    class ValueInterface {

        @Test
        @DisplayName("PASS.value() → \"PASS\"")
        void shouldReturnPassValue() {
            assertThat(VerifyResult.PASS.value()).isEqualTo("PASS");
        }

        @Test
        @DisplayName("PASS.label() → \"一致\"")
        void shouldReturnPassLabel() {
            assertThat(VerifyResult.PASS.label()).isEqualTo("一致");
        }
    }
}
