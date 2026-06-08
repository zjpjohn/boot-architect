package com.cloud.arch.transaction.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cloud.arch.transaction.utils.AsyncTxState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@DisplayName("AsyncTxEvent 异步事务事件")
class AsyncTxEventTest {

    @Nested
    @DisplayName("incrementRetry()")
    class IncrementRetry {

        @Test
        @DisplayName("retries 从 0 → 1")
        void shouldIncrementFromZero() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setRetries(0);
            event.incrementRetry();
            assertThat(event.getRetries()).isEqualTo(1);
        }

        @Test
        @DisplayName("retries 从 3 → 4")
        void shouldIncrementFromThree() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setRetries(3);
            event.incrementRetry();
            assertThat(event.getRetries()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("isDead()")
    class IsDead {

        @Test
        @DisplayName("state=DEAD → true")
        void shouldBeDeadWhenStateIsDead() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.DEAD);
            event.setRetries(0);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isTrue();
        }

        @Test
        @DisplayName("state=FAIL 且 retries >= maxRetry → true")
        void shouldBeDeadWhenFailedAndRetriesExceeded() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.FAIL);
            event.setRetries(5);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isTrue();
        }

        @Test
        @DisplayName("state=FAIL 且 retries < maxRetry → false")
        void shouldNotBeDeadWhenRetriesNotExceeded() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.FAIL);
            event.setRetries(2);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isFalse();
        }

        @Test
        @DisplayName("state=READY → false")
        void shouldNotBeDeadWhenReady() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.READY);
            event.setRetries(0);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isFalse();
        }

        @Test
        @DisplayName("state=RUNNING → false")
        void shouldNotBeDeadWhenRunning() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.RUNNING);
            event.setRetries(0);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isFalse();
        }

        @Test
        @DisplayName("state=SUCCESS → false")
        void shouldNotBeDeadWhenSuccess() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setState(AsyncTxState.SUCCESS);
            event.setRetries(0);
            event.setMaxRetry(5);
            assertThat(event.isDead()).isFalse();
        }
    }

    @Nested
    @DisplayName("calcNextTime()")
    class CalcNextTime {

        @Test
        @DisplayName("retries=0 时 nextTime ≈ 当前时间 + retryInterval")
        void shouldCalcNextTimeWithNoShift() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setRetries(0);
            event.setRetryInterval(10L);
            event.calcNextTime();
            long expected = LocalDateTime.now().plusSeconds(10).until(event.getNextTime(), ChronoUnit.SECONDS);
            assertThat(Math.abs(expected)).isLessThan(2);
        }

        @Test
        @DisplayName("retries=3 时 nextTime ≈ 当前时间 + retryInterval * 8")
        void shouldCalcNextTimeWithExponentialBackoff() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setRetries(3);
            event.setRetryInterval(10L);
            event.calcNextTime();
            long expected = LocalDateTime.now().plusSeconds(80).until(event.getNextTime(), ChronoUnit.SECONDS);
            assertThat(Math.abs(expected)).isLessThan(2);
        }
    }

    @Nested
    @DisplayName("getEventVersion()")
    class GetEventVersion {

        @Test
        @DisplayName("返回 AsyncTxVersion 包装")
        void shouldWrapVersionInAsyncTxVersion() {
            AsyncTxEvent event = new AsyncTxEvent();
            event.setVersion("2.0.0");
            assertThat(event.getEventVersion()).isEqualTo(new AsyncTxVersion("2.0.0"));
        }
    }

    @Nested
    @DisplayName("equals() / hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同 id 和 nextTime → equals true")
        void shouldBeEqualWithSameIdAndNextTime() {
            LocalDateTime now = LocalDateTime.now();
            AsyncTxEvent e1 = new AsyncTxEvent();
            e1.setId(1L);
            e1.setNextTime(now);
            AsyncTxEvent e2 = new AsyncTxEvent();
            e2.setId(1L);
            e2.setNextTime(now);
            assertThat(e1).isEqualTo(e2);
        }

        @Test
        @DisplayName("不同 id → equals false")
        void shouldNotBeEqualWithDifferentId() {
            LocalDateTime now = LocalDateTime.now();
            AsyncTxEvent e1 = new AsyncTxEvent();
            e1.setId(1L);
            e1.setNextTime(now);
            AsyncTxEvent e2 = new AsyncTxEvent();
            e2.setId(2L);
            e2.setNextTime(now);
            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("不同 nextTime → equals false")
        void shouldNotBeEqualWithDifferentNextTime() {
            AsyncTxEvent e1 = new AsyncTxEvent();
            e1.setId(1L);
            e1.setNextTime(LocalDateTime.of(2024, 1, 1, 0, 0));
            AsyncTxEvent e2 = new AsyncTxEvent();
            e2.setId(1L);
            e2.setNextTime(LocalDateTime.of(2024, 6, 1, 0, 0));
            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("相同对象 → hashCode 相同")
        void shouldHaveSameHashCodeWithSameFields() {
            LocalDateTime now = LocalDateTime.now();
            AsyncTxEvent e1 = new AsyncTxEvent();
            e1.setId(1L);
            e1.setNextTime(now);
            AsyncTxEvent e2 = new AsyncTxEvent();
            e2.setId(1L);
            e2.setNextTime(now);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
    }
}
