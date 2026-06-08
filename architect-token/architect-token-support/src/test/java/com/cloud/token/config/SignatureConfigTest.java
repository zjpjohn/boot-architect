package com.cloud.token.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SignatureConfig 签名配置")
class SignatureConfigTest {

    @Nested
    @DisplayName("getNonceExpire()")
    class GetNonceExpire {

        @Test
        @DisplayName("timeDisparity >= 0 → 返回秒数")
        void shouldReturnSecondsWhenNonNegative() {
            SignatureConfig config = new SignatureConfig();
            config.setTimeDisparity(300_000);
            assertThat(config.getNonceExpire()).isEqualTo(300);
        }

        @Test
        @DisplayName("timeDisparity = 0 → 返回 0")
        void shouldReturnZeroWhenZero() {
            SignatureConfig config = new SignatureConfig();
            config.setTimeDisparity(0);
            assertThat(config.getNonceExpire()).isEqualTo(0);
        }

        @Test
        @DisplayName("timeDisparity < 0 → 返回 24 小时")
        void shouldReturn24HoursWhenNegative() {
            SignatureConfig config = new SignatureConfig();
            config.setTimeDisparity(-1);
            assertThat(config.getNonceExpire()).isEqualTo(86400);
        }

        @Test
        @DisplayName("默认值 5 分钟 → 返回 300 秒")
        void shouldUseDefaultFiveMinutes() {
            SignatureConfig config = new SignatureConfig();
            assertThat(config.getNonceExpire()).isEqualTo(300);
        }
    }

    @Nested
    @DisplayName("字段")
    class Fields {

        @Test
        @DisplayName("secretKey 可设置")
        void shouldSetSecretKey() {
            SignatureConfig config = new SignatureConfig();
            config.setSecretKey("my-secret");
            assertThat(config.getSecretKey()).isEqualTo("my-secret");
        }

        @Test
        @DisplayName("timeDisparity 可设置")
        void shouldSetTimeDisparity() {
            SignatureConfig config = new SignatureConfig();
            config.setTimeDisparity(600_000);
            assertThat(config.getTimeDisparity()).isEqualTo(600_000);
        }
    }
}
