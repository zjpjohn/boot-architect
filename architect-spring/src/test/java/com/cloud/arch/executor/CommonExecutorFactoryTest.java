package com.cloud.arch.executor;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CommonExecutorFactory 通用执行器工厂")
class CommonExecutorFactoryTest {

    // 实现了 Executor 的具体类（非接口），用于测试 isInterface 校验
    static class ConcreteExecutor implements Executor<String> {
        @Override public String bizIndex() { return "test"; }
    }

    @Nested
    @DisplayName("构造函数校验")
    class Constructor {

        @Test
        @DisplayName("executorType 必须是接口，传具体类抛异常")
        void shouldRejectNonInterfaceType() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> new CommonExecutorFactory<>(ConcreteExecutor.class))
                    .withMessageContaining("must be interface");
        }

        @Test
        @DisplayName("executorType 为接口时构造成功")
        void shouldAcceptInterfaceType() {
            new CommonExecutorFactory<>(Executor.class);
        }
    }
}
