package com.cloud.arch.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LogRecord 日志记录")
class LogRecordTest {

    @Nested
    @DisplayName("Builder 构造")
    class Builder {

        @Test
        @DisplayName("所有字段通过 builder 正确赋值")
        void shouldBuildAllFields() {
            LogRecord record = LogRecord.builder()
                    .id("log-001")
                    .app("order-service")
                    .group("order")
                    .tenant("tenant-1")
                    .bizNo("ORD-20240101")
                    .operatorId("op-001")
                    .operator("张三")
                    .action("创建订单")
                    .fail(0)
                    .detail("订单详情")
                    .gmtCreate(java.time.LocalDateTime.of(2024, 1, 1, 10, 0))
                    .build();

            assertThat(record.getId()).isEqualTo("log-001");
            assertThat(record.getApp()).isEqualTo("order-service");
            assertThat(record.getGroup()).isEqualTo("order");
            assertThat(record.getTenant()).isEqualTo("tenant-1");
            assertThat(record.getBizNo()).isEqualTo("ORD-20240101");
            assertThat(record.getOperatorId()).isEqualTo("op-001");
            assertThat(record.getOperator()).isEqualTo("张三");
            assertThat(record.getAction()).isEqualTo("创建订单");
            assertThat(record.getFail()).isZero();
            assertThat(record.getDetail()).isEqualTo("订单详情");
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("无参构造 — group/tenant/operatorId/operator/action/detail 默认空字符串")
        void shouldHaveDefaultEmptyStrings() {
            LogRecord record = new LogRecord();
            assertThat(record.getGroup()).isEmpty();
            assertThat(record.getTenant()).isEmpty();
            assertThat(record.getOperatorId()).isEmpty();
            assertThat(record.getOperator()).isEmpty();
            assertThat(record.getAction()).isEmpty();
            assertThat(record.getDetail()).isEmpty();
        }

        @Test
        @DisplayName("fail 默认 0")
        void shouldDefaultFailToZero() {
            LogRecord record = new LogRecord();
            assertThat(record.getFail()).isZero();
        }
    }
}
