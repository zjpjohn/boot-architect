package com.cloud.arch.support.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

@DisplayName("NamedThreadFactory 命名线程工厂")
class NamedThreadFactoryTest {

    @Nested
    @DisplayName("newThread()")
    class NewThread {

        @Test
        @DisplayName("线程名 = prefix + 递增序号")
        void shouldNameThreadsWithPrefixAndSequence() {
            NamedThreadFactory factory = new NamedThreadFactory("log-worker-", false);
            Thread t1 = factory.newThread(() -> {});
            Thread t2 = factory.newThread(() -> {});
            assertThat(t1.getName()).isEqualTo("log-worker-1");
            assertThat(t2.getName()).isEqualTo("log-worker-2");
        }

        @Test
        @DisplayName("daemon=true → 线程为守护线程")
        void shouldCreateDaemonThread() {
            NamedThreadFactory factory = new NamedThreadFactory("daemon-", true);
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.isDaemon()).isTrue();
        }

        @Test
        @DisplayName("daemon=false → 线程为用户线程")
        void shouldCreateUserThread() {
            NamedThreadFactory factory = new NamedThreadFactory("user-", false);
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.isDaemon()).isFalse();
        }

        @Test
        @DisplayName("Runnable 正确绑定")
        void shouldBindRunnableToThread() {
            NamedThreadFactory factory = new NamedThreadFactory("test-", false);
            AtomicReference<String> captured = new AtomicReference<>();
            Thread thread = factory.newThread(() -> captured.set("executed"));
            thread.start();
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertThat(captured.get()).isEqualTo("executed");
        }
    }
}
