package com.cloud.arch.mobile.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SendResult 短信发送结果")
class SendResultTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("success() → success=true, code=200")
        void shouldCreateSuccess() {
            SendResult result = SendResult.success("发送成功");
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getMessage()).isEqualTo("发送成功");
        }

        @Test
        @DisplayName("apiError() → success=false, code=500")
        void shouldCreateApiError() {
            SendResult result = SendResult.apiError("API 异常");
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(500);
        }

        @Test
        @DisplayName("limitError() → success=false, code=600")
        void shouldCreateLimitError() {
            SendResult result = SendResult.limitError("频率限制");
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(600);
        }
    }
}
