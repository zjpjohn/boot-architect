package com.cloud.arch.rocket.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TransactionState 事务状态")
class TransactionStateTest {

    @Nested
    @DisplayName("valueOf() 转换")
    class ValueOf {

        @Test
        @DisplayName("1 → COMMIT")
        void shouldMapCommit() {
            assertThat(TransactionState.valueOf(1)).isEqualTo(Optional.of(TransactionState.COMMIT));
        }

        @Test
        @DisplayName("2 → ROLLBACK")
        void shouldMapRollback() {
            assertThat(TransactionState.valueOf(2)).isEqualTo(Optional.of(TransactionState.ROLLBACK));
        }

        @Test
        @DisplayName("0 → UNKNOWN")
        void shouldMapUnknown() {
            assertThat(TransactionState.valueOf(0)).isEqualTo(Optional.of(TransactionState.UNKNOWN));
        }

        @Test
        @DisplayName("未知值 → Optional.empty()")
        void shouldReturnEmptyForUnknownValue() {
            assertThat(TransactionState.valueOf(99)).isEmpty();
        }
    }

    @Nested
    @DisplayName("状态值")
    class StateValue {

        @Test
        @DisplayName("COMMIT → 1")
        void shouldCommitBeOne() {
            assertThat(TransactionState.COMMIT.getState()).isEqualTo(1);
        }

        @Test
        @DisplayName("ROLLBACK → 2")
        void shouldRollbackBeTwo() {
            assertThat(TransactionState.ROLLBACK.getState()).isEqualTo(2);
        }

        @Test
        @DisplayName("UNKNOWN → 0")
        void shouldUnknownBeZero() {
            assertThat(TransactionState.UNKNOWN.getState()).isEqualTo(0);
        }
    }
}
