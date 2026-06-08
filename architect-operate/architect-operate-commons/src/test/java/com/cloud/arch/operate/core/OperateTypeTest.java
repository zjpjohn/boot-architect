package com.cloud.arch.operate.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OperateType 操作类型")
class OperateTypeTest {

    @Nested
    @DisplayName("value() / label()")
    class ValueAndLabel {

        @Test
        @DisplayName("ADD → \"add\" / \"新增\"")
        void shouldReturnAdd() {
            assertThat(OperateType.ADD.value()).isEqualTo("add");
            assertThat(OperateType.ADD.label()).isEqualTo("新增");
        }

        @Test
        @DisplayName("EDIT → \"edit\" / \"修改\"")
        void shouldReturnEdit() {
            assertThat(OperateType.EDIT.value()).isEqualTo("edit");
            assertThat(OperateType.EDIT.label()).isEqualTo("修改");
        }

        @Test
        @DisplayName("DELETE → \"delete\" / \"删除\"")
        void shouldReturnDelete() {
            assertThat(OperateType.DELETE.value()).isEqualTo("delete");
            assertThat(OperateType.DELETE.label()).isEqualTo("删除");
        }
    }

    @Nested
    @DisplayName("of() 转换")
    class Of {

        @Test
        @DisplayName("已知 type → 对应枚举")
        void shouldMapKnownType() {
            assertThat(OperateType.of("add")).isEqualTo(OperateType.ADD);
            assertThat(OperateType.of("edit")).isEqualTo(OperateType.EDIT);
            assertThat(OperateType.of("import")).isEqualTo(OperateType.IMPORT);
        }

        @Test
        @DisplayName("未知 type → OTHER")
        void shouldDefaultToOther() {
            assertThat(OperateType.of("unknown_type")).isEqualTo(OperateType.OTHER);
        }
    }
}
