package com.cloud.arch.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Operator 操作者")
class OperatorTest {

    @Nested
    @DisplayName("record 字段")
    class RecordFields {

        @Test
        @DisplayName("operatorId 和 operator 正确赋值")
        void shouldSetFields() {
            Operator op = new Operator("001", "张三");
            assertThat(op.operatorId()).isEqualTo("001");
            assertThat(op.operator()).isEqualTo("张三");
        }
    }
}
