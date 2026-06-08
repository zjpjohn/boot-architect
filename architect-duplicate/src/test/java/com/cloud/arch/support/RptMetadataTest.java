package com.cloud.arch.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RptMetadata 重复校验元数据")
class RptMetadataTest {

    @Nested
    @DisplayName("nonNull()")
    class NonNull {

        @Test
        @DisplayName("非空字符串 → true")
        void shouldReturnTrueForNonBlankString() {
            assertThat(RptMetadata.nonNull("hello")).isTrue();
        }

        @Test
        @DisplayName("空字符串 → false")
        void shouldReturnFalseForEmptyString() {
            assertThat(RptMetadata.nonNull("")).isFalse();
        }

        @Test
        @DisplayName("空白字符串 → false")
        void shouldReturnFalseForBlankString() {
            assertThat(RptMetadata.nonNull("   ")).isFalse();
        }

        @Test
        @DisplayName("非 String 非 null → true")
        void shouldReturnTrueForNonNullObject() {
            assertThat(RptMetadata.nonNull(123)).isTrue();
            assertThat(RptMetadata.nonNull(0L)).isTrue();
        }

        @Test
        @DisplayName("null → false")
        void shouldReturnFalseForNull() {
            assertThat(RptMetadata.nonNull(null)).isFalse();
        }
    }
}
