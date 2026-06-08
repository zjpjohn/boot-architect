package com.cloud.arch.transaction.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncTxState 异步事务状态常量")
class AsyncTxStateTest {

    @Nested
    @DisplayName("状态常量")
    class Constants {

        @Test
        @DisplayName("READY = 1")
        void shouldHaveCorrectReadyValue() {
            assertThat(AsyncTxState.READY).isEqualTo(1);
        }

        @Test
        @DisplayName("RUNNING = 2")
        void shouldHaveCorrectRunningValue() {
            assertThat(AsyncTxState.RUNNING).isEqualTo(2);
        }

        @Test
        @DisplayName("SUCCESS = 3")
        void shouldHaveCorrectSuccessValue() {
            assertThat(AsyncTxState.SUCCESS).isEqualTo(3);
        }

        @Test
        @DisplayName("FAIL = 4")
        void shouldHaveCorrectFailValue() {
            assertThat(AsyncTxState.FAIL).isEqualTo(4);
        }

        @Test
        @DisplayName("DEAD = 5")
        void shouldHaveCorrectDeadValue() {
            assertThat(AsyncTxState.DEAD).isEqualTo(5);
        }
    }
}
