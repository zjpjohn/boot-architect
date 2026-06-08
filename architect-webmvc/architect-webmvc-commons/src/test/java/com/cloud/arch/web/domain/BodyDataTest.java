package com.cloud.arch.web.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BodyData 响应体")
class BodyDataTest {

    @Nested
    @DisplayName("构造函数")
    class Constructors {

        @Test
        @DisplayName("四参构造默认 error=null")
        void shouldDefaultErrorToNull() {
            BodyData<String> data = new BodyData<>("success", 200, "result");
            assertThat(data.error()).isNull();
            assertThat(data.message()).isEqualTo("success");
            assertThat(data.code()).isEqualTo(200);
            assertThat(data.data()).isEqualTo("result");
            assertThat(data.timestamp()).isPositive();
        }

        @Test
        @DisplayName("二参构造默认 message 和 data 为 null")
        void shouldDefaultMessageAndDataToNull() {
            BodyData<String> data = new BodyData<>("error msg", 500);
            assertThat(data.error()).isEqualTo("error msg");
            assertThat(data.code()).isEqualTo(500);
            assertThat(data.message()).isNull();
            assertThat(data.data()).isNull();
        }

        @Test
        @DisplayName("error/message/code 三参构造 data 为 null")
        void shouldDefaultDataToNull() {
            BodyData<String> data = new BodyData<>("err", "msg", 400);
            assertThat(data.error()).isEqualTo("err");
            assertThat(data.message()).isEqualTo("msg");
            assertThat(data.code()).isEqualTo(400);
            assertThat(data.data()).isNull();
        }
    }
}
