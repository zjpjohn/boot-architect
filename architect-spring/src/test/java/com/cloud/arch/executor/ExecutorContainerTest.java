package com.cloud.arch.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExecutorContainer 执行器容器")
class ExecutorContainerTest {

    private ExecutorFactory.ExecutorContainer<String, TestStringExecutor> container;

    static class TestStringExecutor implements Executor<String> {
        private final String key;

        TestStringExecutor(String key) { this.key = key; }

        @Override public String bizIndex() { return key; }
    }

    @BeforeEach
    void setUp() {
        List<TestStringExecutor> beans = List.of(
                new TestStringExecutor("alpha"),
                new TestStringExecutor("beta"),
                new TestStringExecutor("gamma")
        );
        container = new ExecutorFactory.ExecutorContainer<>(beans);
    }

    @Nested
    @DisplayName("of 按 key 获取")
    class Of {

        @Test
        @DisplayName("存在的 key 返回对应执行器")
        void shouldReturnMatchingExecutor() {
            assertThat(container.of("alpha").bizIndex()).isEqualTo("alpha");
            assertThat(container.of("beta").bizIndex()).isEqualTo("beta");
        }

        @Test
        @DisplayName("不存在的 key 抛出 NullPointerException")
        void shouldThrowWhenKeyNotFound() {
            assertThatThrownBy(() -> container.of("unknown"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ofNullable 安全获取")
    class OfNullable {

        @Test
        @DisplayName("存在的 key 返回 Optional 有值")
        void shouldReturnPresentOptional() {
            Optional<TestStringExecutor> result = container.ofNullable("alpha");
            assertThat(result).isPresent();
            assertThat(result.get().bizIndex()).isEqualTo("alpha");
        }

        @Test
        @DisplayName("不存在的 key 返回 Optional.empty")
        void shouldReturnEmptyOptional() {
            assertThat(container.ofNullable("missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("集合查询")
    class CollectionQueries {

        @Test
        @DisplayName("executors 返回全部执行器")
        void shouldReturnAllExecutors() {
            Collection<TestStringExecutor> all = container.executors();
            assertThat(all).hasSize(3);
            assertThat(all.stream().map(Executor::bizIndex))
                    .containsExactlyInAnyOrder("alpha", "beta", "gamma");
        }

        @Test
        @DisplayName("keys 返回全部 key")
        void shouldReturnAllKeys() {
            Set<String> keys = container.keys();
            assertThat(keys).containsExactlyInAnyOrder("alpha", "beta", "gamma");
        }
    }
}
