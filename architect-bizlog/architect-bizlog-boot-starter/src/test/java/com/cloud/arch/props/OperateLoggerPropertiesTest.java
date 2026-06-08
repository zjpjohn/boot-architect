package com.cloud.arch.props;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@DisplayName("OperateLoggerProperties 操作日志配置属性")
class OperateLoggerPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("async = false")
        void shouldDefaultAsyncFalse() {
            assertThat(new OperateLoggerProperties().getAsync()).isFalse();
        }

        @Test
        @DisplayName("coreThreads = 1")
        void shouldDefaultCoreThreads() {
            assertThat(new OperateLoggerProperties().getCoreThreads()).isEqualTo(1);
        }

        @Test
        @DisplayName("maxThreads = 2")
        void shouldDefaultMaxThreads() {
            assertThat(new OperateLoggerProperties().getMaxThreads()).isEqualTo(2);
        }

        @Test
        @DisplayName("batchSize = 20")
        void shouldDefaultBatchSize() {
            assertThat(new OperateLoggerProperties().getBatchSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("timeout = 5 秒")
        void shouldDefaultTimeout() {
            assertThat(new OperateLoggerProperties().getTimeout()).isEqualTo(Duration.ofSeconds(5));
        }
    }

    @Nested
    @DisplayName("setter")
    class Setters {

        @Test
        @DisplayName("字段可自定义")
        void shouldOverrideDefaults() {
            OperateLoggerProperties props = new OperateLoggerProperties();
            props.setAsync(true);
            props.setCoreThreads(4);
            props.setMaxThreads(8);
            props.setBatchSize(50);
            assertThat(props.getAsync()).isTrue();
            assertThat(props.getCoreThreads()).isEqualTo(4);
            assertThat(props.getMaxThreads()).isEqualTo(8);
            assertThat(props.getBatchSize()).isEqualTo(50);
        }
    }
}
