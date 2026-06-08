package com.cloud.arch.rocket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConsumeMode 消费模式")
class ConsumeModeTest {

    @Nested
    @DisplayName("枚举值")
    class Values {

        @Test
        @DisplayName("包含 CONCURRENTLY")
        void shouldContainConcurrently() {
            assertThat(ConsumeMode.valueOf("CONCURRENTLY")).isEqualTo(ConsumeMode.CONCURRENTLY);
        }

        @Test
        @DisplayName("包含 ORDERLY")
        void shouldContainOrderly() {
            assertThat(ConsumeMode.valueOf("ORDERLY")).isEqualTo(ConsumeMode.ORDERLY);
        }
    }
}
