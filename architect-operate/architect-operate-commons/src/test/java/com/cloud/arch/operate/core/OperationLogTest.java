package com.cloud.arch.operate.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OperationLog 操作日志")
class OperationLogTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            OperationLog log = new OperationLog();
            log.setId(100L);
            log.setAppNo("app-01");
            log.setTenantId("tenant-01");
            log.setBizGroup("order");
            log.setTitle("创建订单");
            log.setType(OperateType.ADD);
            log.setTarget("OrderService.createOrder");
            log.setMethod("POST");
            log.setReqUri("/api/orders");
            log.setOpId(1L);
            log.setOpName("张三");
            log.setOpIp("192.168.1.1");
            log.setState(1);
            log.setTakenTime(150L);

            assertThat(log.getId()).isEqualTo(100L);
            assertThat(log.getAppNo()).isEqualTo("app-01");
            assertThat(log.getTenantId()).isEqualTo("tenant-01");
            assertThat(log.getBizGroup()).isEqualTo("order");
            assertThat(log.getTitle()).isEqualTo("创建订单");
            assertThat(log.getType()).isEqualTo(OperateType.ADD);
            assertThat(log.getTarget()).isEqualTo("OrderService.createOrder");
            assertThat(log.getMethod()).isEqualTo("POST");
            assertThat(log.getReqUri()).isEqualTo("/api/orders");
            assertThat(log.getOpId()).isEqualTo(1L);
            assertThat(log.getOpName()).isEqualTo("张三");
            assertThat(log.getOpIp()).isEqualTo("192.168.1.1");
            assertThat(log.getState()).isEqualTo(1);
            assertThat(log.getTakenTime()).isEqualTo(150L);
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("新实例字段为 null")
        void shouldHaveNullFieldsByDefault() {
            OperationLog log = new OperationLog();
            assertThat(log.getId()).isNull();
            assertThat(log.getAppNo()).isNull();
            assertThat(log.getType()).isNull();
            assertThat(log.getState()).isNull();
        }
    }
}
