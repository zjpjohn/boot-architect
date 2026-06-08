package com.cloud.arch.event.core.publish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventState 事件状态")
class EventStateTest {

    @Nested
    @DisplayName("of() 转换")
    class Of {

        @Test
        @DisplayName("0 → INITIALIZED")
        void shouldMapInitialized() {
            assertThat(EventState.of(0)).isEqualTo(EventState.INITIALIZED);
        }

        @Test
        @DisplayName("1 → SUCCEEDED")
        void shouldMapSucceeded() {
            assertThat(EventState.of(1)).isEqualTo(EventState.SUCCEEDED);
        }

        @Test
        @DisplayName("2 → FAILED")
        void shouldMapFailed() {
            assertThat(EventState.of(2)).isEqualTo(EventState.FAILED);
        }

        @Test
        @DisplayName("未知值 → 抛出异常")
        void shouldThrowForUnknownValue() {
            assertThatThrownBy(() -> EventState.of(99))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("状态值")
    class StateValue {

        @Test
        @DisplayName("INITIALIZED → 0")
        void shouldHaveStateZero() {
            assertThat(EventState.INITIALIZED.getState()).isEqualTo(0);
        }

        @Test
        @DisplayName("SUCCEEDED → 1")
        void shouldHaveStateOne() {
            assertThat(EventState.SUCCEEDED.getState()).isEqualTo(1);
        }

        @Test
        @DisplayName("FAILED → 2")
        void shouldHaveStateTwo() {
            assertThat(EventState.FAILED.getState()).isEqualTo(2);
        }
    }
}
