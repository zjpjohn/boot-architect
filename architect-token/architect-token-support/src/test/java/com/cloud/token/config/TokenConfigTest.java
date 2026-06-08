package com.cloud.token.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenConfig Token 配置")
class TokenConfigTest {

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("tokenName = \"tk\"")
        void shouldHaveDefaultTokenName() {
            assertThat(new TokenConfig().getTokenName()).isEqualTo("tk");
        }

        @Test
        @DisplayName("timeout = 7 天(秒)")
        void shouldHaveDefaultTimeout() {
            assertThat(new TokenConfig().getTimeout()).isEqualTo(60 * 60 * 24 * 7L);
        }

        @Test
        @DisplayName("activeTimeout = -1")
        void shouldHaveDefaultActiveTimeout() {
            assertThat(new TokenConfig().getActiveTimeout()).isEqualTo(-1);
        }

        @Test
        @DisplayName("concurrent = true")
        void shouldHaveDefaultConcurrent() {
            assertThat(new TokenConfig().isConcurrent()).isTrue();
        }

        @Test
        @DisplayName("share = true")
        void shouldHaveDefaultShare() {
            assertThat(new TokenConfig().isShare()).isTrue();
        }

        @Test
        @DisplayName("maxLoginCount = 12")
        void shouldHaveDefaultMaxLoginCount() {
            assertThat(new TokenConfig().getMaxLoginCount()).isEqualTo(12);
        }

        @Test
        @DisplayName("maxRetryTimes = 6")
        void shouldHaveDefaultMaxRetryTimes() {
            assertThat(new TokenConfig().getMaxRetryTimes()).isEqualTo(6);
        }

        @Test
        @DisplayName("tokenStyle = \"uuid\"")
        void shouldHaveDefaultTokenStyle() {
            assertThat(new TokenConfig().getTokenStyle()).isEqualTo("uuid");
        }

        @Test
        @DisplayName("autoRenew = true")
        void shouldHaveDefaultAutoRenew() {
            assertThat(new TokenConfig().isAutoRenew()).isTrue();
        }

        @Test
        @DisplayName("dynamicActiveTimeout = false")
        void shouldHaveDefaultDynamicActiveTimeout() {
            assertThat(new TokenConfig().isDynamicActiveTimeout()).isFalse();
        }

        @Test
        @DisplayName("authExcludes = \"\"")
        void shouldHaveDefaultAuthExcludes() {
            assertThat(new TokenConfig().getAuthExcludes()).isEmpty();
        }

        @Test
        @DisplayName("headerPrefix = \"\"")
        void shouldHaveDefaultHeaderPrefix() {
            assertThat(new TokenConfig().getHeaderPrefix()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ErrorCode 内部类")
    class ErrorCodeDefaults {

        @Test
        @DisplayName("auth = 10401")
        void shouldHaveDefaultAuthCode() {
            assertThat(new TokenConfig().getCode().getAuth()).isEqualTo(10401);
        }

        @Test
        @DisplayName("security = 10403")
        void shouldHaveDefaultSecurityCode() {
            assertThat(new TokenConfig().getCode().getSecurity()).isEqualTo(10403);
        }

        @Test
        @DisplayName("muted = 11403")
        void shouldHaveDefaultMutedCode() {
            assertThat(new TokenConfig().getCode().getMuted()).isEqualTo(11403);
        }

        @Test
        @DisplayName("dual = 11401")
        void shouldHaveDefaultDualCode() {
            assertThat(new TokenConfig().getCode().getDual()).isEqualTo(11401);
        }

        @Test
        @DisplayName("error = 10500")
        void shouldHaveDefaultErrorCode() {
            assertThat(new TokenConfig().getCode().getError()).isEqualTo(10500);
        }
    }

    @Nested
    @DisplayName("字段 setter")
    class Setters {

        @Test
        @DisplayName("所有字段可自定义")
        void shouldOverrideDefaults() {
            TokenConfig config = new TokenConfig();
            config.setTokenName("custom-tk");
            config.setTimeout(3600L);
            config.setConcurrent(false);
            config.setShare(false);
            config.setMaxLoginCount(5);
            config.setMaxRetryTimes(3);
            config.setTokenStyle("random-32");

            assertThat(config.getTokenName()).isEqualTo("custom-tk");
            assertThat(config.getTimeout()).isEqualTo(3600L);
            assertThat(config.isConcurrent()).isFalse();
            assertThat(config.isShare()).isFalse();
            assertThat(config.getMaxLoginCount()).isEqualTo(5);
        }
    }
}
