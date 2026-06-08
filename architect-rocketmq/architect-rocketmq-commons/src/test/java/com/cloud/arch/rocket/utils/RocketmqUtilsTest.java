package com.cloud.arch.rocket.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RocketmqUtils RocketMQ 工具")
class RocketmqUtilsTest {

    @Nested
    @DisplayName("isValidTag()")
    class IsValidTag {

        @Test
        @DisplayName("有意义的标签 → true")
        void shouldAcceptMeaningfulTag() {
            assertThat(RocketmqUtils.isValidTag("order_created")).isTrue();
        }

        @Test
        @DisplayName("空字符串 → false")
        void shouldRejectEmpty() {
            assertThat(RocketmqUtils.isValidTag("")).isFalse();
        }

        @Test
        @DisplayName("null → false")
        void shouldRejectNull() {
            assertThat(RocketmqUtils.isValidTag(null)).isFalse();
        }

        @Test
        @DisplayName("通配符 \"*\" → false")
        void shouldRejectWildcard() {
            assertThat(RocketmqUtils.isValidTag("*")).isFalse();
        }

        @Test
        @DisplayName("包含 \"||\" → false")
        void shouldRejectDelimiter() {
            assertThat(RocketmqUtils.isValidTag("tag1||tag2")).isFalse();
        }
    }
}
