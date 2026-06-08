package com.cloud.arch.executor;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EnumExecutorFactory 枚举执行器工厂")
class EnumExecutorFactoryTest {

    enum TestEnum { A, B, C }

    interface TestEnumExecutor extends Executor<TestEnum> { }

    // 实现了 TestEnumExecutor 的具体类（非接口），用于测试 isInterface 校验
    static class ConcreteEnumExecutor implements TestEnumExecutor {
        @Override public TestEnum bizIndex() { return TestEnum.A; }
    }

    @Nested
    @DisplayName("构造函数校验")
    class Constructor {

        @Test
        @DisplayName("executorType 必须是接口，传具体类抛异常")
        void shouldRejectNonInterfaceType() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> new EnumExecutorFactory<>(TestEnum.class, ConcreteEnumExecutor.class))
                    .withMessageContaining("must be interface");
        }

        @Test
        @DisplayName("executorType 为接口时构造成功")
        void shouldAcceptInterfaceType() {
            new EnumExecutorFactory<>(TestEnum.class, TestEnumExecutor.class);
        }
    }
}
