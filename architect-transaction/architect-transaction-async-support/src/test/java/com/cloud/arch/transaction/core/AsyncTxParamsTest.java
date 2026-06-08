package com.cloud.arch.transaction.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

@DisplayName("AsyncTxParams 异步事务参数")
class AsyncTxParamsTest {

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("非空数组 → 按位置存入")
        void shouldStoreByPosition() {
            AsyncTxParams params = new AsyncTxParams(new Object[]{"a", 123, true});
            assertThat(params).hasSize(3);
            assertThat(params.get(0)).isEqualTo("a");
            assertThat(params.get(1)).isEqualTo(123);
            assertThat(params.get(2)).isEqualTo(true);
        }

        @Test
        @DisplayName("空数组 → size 为 0")
        void shouldBeEmptyForEmptyArray() {
            AsyncTxParams params = new AsyncTxParams(new Object[]{});
            assertThat(params).isEmpty();
        }

        @Test
        @DisplayName("null → size 为 0")
        void shouldBeEmptyForNull() {
            AsyncTxParams params = new AsyncTxParams(null);
            assertThat(params).isEmpty();
        }

        @Test
        @DisplayName("无参构造 → size 为 0")
        void shouldBeEmptyWithNoArgsConstructor() {
            AsyncTxParams params = new AsyncTxParams();
            assertThat(params).isEmpty();
        }
    }

    @Nested
    @DisplayName("jsonArguments()")
    class JsonArguments {

        @Test
        @DisplayName("根据方法参数类型反序列化")
        void shouldDeserializeByMethodParamTypes() throws Exception {
            Method method = SampleService.class.getMethod("doWork", String.class, Integer.class);
            AsyncTxParams params = new AsyncTxParams(new Object[]{"hello", 42});
            Object[] result = params.jsonArguments(method);
            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo("hello");
            assertThat(result[1]).isEqualTo(42);
        }

        @Test
        @DisplayName("无参方法 → 返回空数组")
        void shouldReturnEmptyForNoArgMethod() throws Exception {
            Method method = SampleService.class.getMethod("noArgs");
            AsyncTxParams params = new AsyncTxParams(new Object[]{"hello"});
            Object[] result = params.jsonArguments(method);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("参数个数少于方法参数 → 缺失位置为 null")
        void shouldFillMissingWithNull() throws Exception {
            Method method = SampleService.class.getMethod("threeArgs", String.class, Integer.class, Boolean.class);
            AsyncTxParams params = new AsyncTxParams(new Object[]{"hello"});
            Object[] result = params.jsonArguments(method);
            assertThat(result).hasSize(3);
            assertThat(result[0]).isEqualTo("hello");
            assertThat(result[1]).isNull();
            assertThat(result[2]).isNull();
        }

        @Test
        @DisplayName("方法参数多于实际参数 → 按 min 长度截断")
        void shouldTruncateExtraMethodParams() throws Exception {
            Method method = SampleService.class.getMethod("threeArgs", String.class, Integer.class, Boolean.class);
            AsyncTxParams params = new AsyncTxParams(new Object[]{"a", 1, true, "extra"});
            Object[] result = params.jsonArguments(method);
            assertThat(result).hasSize(3);
            assertThat(result[0]).isEqualTo("a");
            assertThat(result[1]).isEqualTo(1);
            assertThat(result[2]).isEqualTo(true);
        }
    }

    @SuppressWarnings("unused")
    static class SampleService {
        public void doWork(String name, Integer count) {}
        public void noArgs() {}
        public void threeArgs(String name, Integer count, Boolean enabled) {}
    }
}
