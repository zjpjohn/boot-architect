package com.cloud.arch.transaction.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncTxSharding 异步事务分片键")
class AsyncTxShardingTest {

    @AfterEach
    void tearDown() {
        AsyncTxSharding.clear();
    }

    @Nested
    @DisplayName("shardingKey() 设置/获取")
    class SetAndGet {

        @Test
        @DisplayName("设置后能获取到")
        void shouldGetAfterSet() {
            AsyncTxSharding.shardingKey("db_001");
            assertThat(AsyncTxSharding.shardingKey()).isEqualTo("db_001");
        }

        @Test
        @DisplayName("未设置时返回空字符串")
        void shouldReturnEmptyByDefault() {
            assertThat(AsyncTxSharding.shardingKey()).isEqualTo("");
        }

        @Test
        @DisplayName("null → 抛异常")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> AsyncTxSharding.shardingKey(null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("空字符串 → 抛异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> AsyncTxSharding.shardingKey(""))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("空白字符串 → 抛异常")
        void shouldThrowForBlank() {
            assertThatThrownBy(() -> AsyncTxSharding.shardingKey("  "))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("清空后恢复默认值")
        void shouldResetToEmptyAfterClear() {
            AsyncTxSharding.shardingKey("db_001");
            AsyncTxSharding.clear();
            assertThat(AsyncTxSharding.shardingKey()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("线程隔离")
    class ThreadIsolation {

        @Test
        @DisplayName("不同线程设置不同值 → 互不影响")
        void shouldIsolateBetweenThreads() throws Exception {
            AsyncTxSharding.shardingKey("main_thread");
            Thread other = new Thread(() -> AsyncTxSharding.shardingKey("other_thread"));
            other.start();
            other.join();
            assertThat(AsyncTxSharding.shardingKey()).isEqualTo("main_thread");
        }
    }
}
