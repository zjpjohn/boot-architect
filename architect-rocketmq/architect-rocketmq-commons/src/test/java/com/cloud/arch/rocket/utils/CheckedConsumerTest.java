package com.cloud.arch.rocket.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CheckedConsumer 受检消费者")
class CheckedConsumerTest {

    @Nested
    @DisplayName("apply() 包装")
    class Apply {

        @Test
        @DisplayName("正常执行 → Consumer 接受值")
        void shouldCallConsumer() {
            AtomicReference<String> ref = new AtomicReference<>();
            Consumer<String> consumer = CheckedConsumer.apply(t -> ref.set(t));
            consumer.accept("hello");
            assertThat(ref.get()).isEqualTo("hello");
        }

        @Test
        @DisplayName("checked exception → RuntimeException")
        void shouldWrapCheckedException() {
            Consumer<String> consumer = CheckedConsumer.apply(t -> {
                throw new Exception("checked error");
            });
            assertThatThrownBy(() -> consumer.accept("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("null consumer → NullPointerException")
        void shouldRejectNull() {
            assertThatThrownBy(() -> CheckedConsumer.apply(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
