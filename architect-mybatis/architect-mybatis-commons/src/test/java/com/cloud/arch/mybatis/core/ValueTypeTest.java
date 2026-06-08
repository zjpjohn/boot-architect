package com.cloud.arch.mybatis.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ValueType 值类型")
class ValueTypeTest {

    @Nested
    @DisplayName("findOf()")
    class FindOf {

        @Test
        @DisplayName("已知名称 → 对应 ValueType")
        void shouldFindByTypeName() {
            assertThat(ValueType.findOf("java.lang.String")).isEqualTo(ValueType.STRING);
            assertThat(ValueType.findOf("java.lang.Integer")).isEqualTo(ValueType.INT);
            assertThat(ValueType.findOf("java.lang.Long")).isEqualTo(ValueType.LONG);
            assertThat(ValueType.findOf("java.lang.Double")).isEqualTo(ValueType.DOUBLE);
            assertThat(ValueType.findOf("java.lang.Float")).isEqualTo(ValueType.FLOAT);
            assertThat(ValueType.findOf("java.lang.Short")).isEqualTo(ValueType.SHORT);
        }

        @Test
        @DisplayName("未知名称 → null")
        void shouldReturnNullForUnknown() {
            assertThat(ValueType.findOf("java.math.BigDecimal")).isNull();
        }
    }

    @Nested
    @DisplayName("jdbcType() 映射")
    class JdbcTypeMapping {

        @Test
        @DisplayName("STRING → VARCHAR")
        void shouldMapStringToVarchar() {
            assertThat(ValueType.STRING.jdbcType()).isEqualTo(JdbcType.VARCHAR);
        }

        @Test
        @DisplayName("INT → INTEGER")
        void shouldMapIntToInteger() {
            assertThat(ValueType.INT.jdbcType()).isEqualTo(JdbcType.INTEGER);
        }

        @Test
        @DisplayName("LONG → BIGINT")
        void shouldMapLongToBigint() {
            assertThat(ValueType.LONG.jdbcType()).isEqualTo(JdbcType.BIGINT);
        }

        @Test
        @DisplayName("DOUBLE → DOUBLE")
        void shouldMapDoubleToDouble() {
            assertThat(ValueType.DOUBLE.jdbcType()).isEqualTo(JdbcType.DOUBLE);
        }
    }
}
