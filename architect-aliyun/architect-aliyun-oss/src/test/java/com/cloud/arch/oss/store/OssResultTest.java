package com.cloud.arch.oss.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OssResult OSS 存储结果")
class OssResultTest {

    @Nested
    @DisplayName("Builder 构造")
    class Builder {

        @Test
        @DisplayName("所有字段通过 builder 正确赋值")
        void shouldBuildAllFields() {
            OssResult result = OssResult.builder()
                    .key("images/avatar.jpg")
                    .url("https://oss.example.com/images/avatar.jpg")
                    .type(1)
                    .build();

            assertThat(result.getKey()).isEqualTo("images/avatar.jpg");
            assertThat(result.getUrl()).isEqualTo("https://oss.example.com/images/avatar.jpg");
            assertThat(result.getType()).isEqualTo(1);
        }
    }
}
