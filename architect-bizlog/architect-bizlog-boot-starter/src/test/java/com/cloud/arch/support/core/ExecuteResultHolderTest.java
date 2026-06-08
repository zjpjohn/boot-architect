package com.cloud.arch.support.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExecuteResultHolder 执行结果持有者")
class ExecuteResultHolderTest {

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("单参 success=true → throwable=null, message=\"\"")
        void shouldConstructSuccessResult() {
            ExecuteResultHolder result = new ExecuteResultHolder(true);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getThrowable()).isNull();
            assertThat(result.getMessage()).isEmpty();
        }

        @Test
        @DisplayName("单参 success=false → throwable=null, message=\"\"")
        void shouldConstructFailureResult() {
            ExecuteResultHolder result = new ExecuteResultHolder(false);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getThrowable()).isNull();
            assertThat(result.getMessage()).isEmpty();
        }

        @Test
        @DisplayName("双参带异常 → message 从异常提取")
        void shouldConstructWithThrowable() {
            RuntimeException ex = new RuntimeException("something went wrong");
            ExecuteResultHolder result = new ExecuteResultHolder(false, ex);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getThrowable()).isSameAs(ex);
            assertThat(result.getMessage()).isEqualTo("something went wrong");
        }

        @Test
        @DisplayName("无参构造 → 默认值")
        void shouldHaveDefaultsWithNoArgsConstructor() {
            ExecuteResultHolder result = new ExecuteResultHolder();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getThrowable()).isNull();
            assertThat(result.getMessage()).isNull();
        }
    }
}
