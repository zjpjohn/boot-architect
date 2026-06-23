package com.cloud.arch.operate.props;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@DisplayName("OperateLogProperties 操作日志配置属性")
class OperateLogPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("batchSize = 20")
        void shouldDefaultBatchSize() {
            assertThat(new OperateLogProperties().getBatchSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("timeout = 2 秒")
        void shouldDefaultTimeout() {
            assertThat(new OperateLogProperties().getTimeout()).isEqualTo(Duration.ofSeconds(2));
        }

        @Test
        @DisplayName("excludes = \"\"")
        void shouldDefaultExcludesEmpty() {
            assertThat(new OperateLogProperties().getExcludes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("excludeList()")
    class ExcludeList {

        @Test
        @DisplayName("空字符串 → 含空字符串的单元素列表")
        void shouldReturnSingleEmptyElementForEmptyString() {
            OperateLogProperties props = new OperateLogProperties();
            assertThat(props.excludeList()).containsExactly("");
        }

        @Test
        @DisplayName("单个字段 → 单元素列表")
        void shouldReturnSingleElement() {
            OperateLogProperties props = new OperateLogProperties();
            props.setExcludes("password");
            assertThat(props.excludeList()).containsExactly("password");
        }

        @Test
        @DisplayName("多个逗号分隔 → 多元素列表")
        void shouldSplitByComma() {
            OperateLogProperties props = new OperateLogProperties();
            props.setExcludes("password,token,secret");
            assertThat(props.excludeList()).containsExactly("password", "token", "secret");
        }

        @Test
        @DisplayName("逗号分隔自动 trim")
        void shouldTrimWhitespace() {
            OperateLogProperties props = new OperateLogProperties();
            props.setExcludes("password , token , secret");
            assertThat(props.excludeList()).containsExactly("password", "token", "secret");
        }
    }
}
