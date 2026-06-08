package com.cloud.arch.transaction.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncTxInvokers 异步事务调用器注册表")
class AsyncTxInvokersTest {

    @BeforeEach
    void setUp() {
        // 清理注册表（通过反射清空 ConcurrentHashMap 不可行，
        // 但每个测试使用不同的 key 可以避免冲突）
    }

    @Nested
    @DisplayName("add() / get()")
    class AddAndGet {

        @Test
        @DisplayName("添加后能获取到")
        void shouldGetAfterAdd() {
            AsyncTxInvoker invoker = mockInvoker("testKey");
            AsyncTxInvokers.add(invoker);
            assertThat(AsyncTxInvokers.get("testKey")).isSameAs(invoker);
        }

        @Test
        @DisplayName("重复 key → 抛异常")
        void shouldThrowForDuplicateKey() {
            AsyncTxInvoker invoker1 = mockInvoker("dupKey");
            AsyncTxInvoker invoker2 = mockInvoker("dupKey");
            AsyncTxInvokers.add(invoker1);
            assertThatThrownBy(() -> AsyncTxInvokers.add(invoker2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Has exist same key");
        }

        @Test
        @DisplayName("不存在的 key → null")
        void shouldReturnNullForMissingKey() {
            assertThat(AsyncTxInvokers.get("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("asyncKey() 静态方法")
    class AsyncKeyStatic {

        @Test
        @DisplayName("无 extra → ClassName.methodName")
        void shouldBuildKeyWithoutExtra() throws Exception {
            String key = AsyncTxInvoker.asyncKey(SampleService.class,
                    SampleService.class.getMethod("doWork"), null);
            assertThat(key).isEqualTo("SampleService.doWork");
        }

        @Test
        @DisplayName("extra 为空字符串 → ClassName.methodName")
        void shouldBuildKeyWithEmptyExtra() throws Exception {
            String key = AsyncTxInvoker.asyncKey(SampleService.class,
                    SampleService.class.getMethod("doWork"), "");
            assertThat(key).isEqualTo("SampleService.doWork");
        }

        @Test
        @DisplayName("有 extra → ClassName.methodName.extra")
        void shouldBuildKeyWithExtra() throws Exception {
            String key = AsyncTxInvoker.asyncKey(SampleService.class,
                    SampleService.class.getMethod("doWork"), "variantA");
            assertThat(key).isEqualTo("SampleService.doWork.variantA");
        }
    }

    private static AsyncTxInvoker mockInvoker(String key) {
        AsyncTxInvoker invoker = mock(AsyncTxInvoker.class);
        when(invoker.getKey()).thenReturn(key);
        return invoker;
    }

    @SuppressWarnings("unused")
    static class SampleService {
        public void doWork() {}
    }
}
