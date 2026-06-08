package com.cloud.arch.transaction.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncTxVersion 异步事务版本")
class AsyncTxVersionTest {

    @Nested
    @DisplayName("构造与解析")
    class Construction {

        @Test
        @DisplayName("正常版本号 → cells 正确解析")
        void shouldParseVersionCells() {
            AsyncTxVersion v = new AsyncTxVersion("1.2.3");
            assertThat(v.getVersion()).isEqualTo("1.2.3");
            assertThat(v.getCells()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("单段版本号 → cells 单元数组")
        void shouldParseSingleSegment() {
            AsyncTxVersion v = new AsyncTxVersion("5");
            assertThat(v.getCells()).containsExactly(5);
        }

        @Test
        @DisplayName("空字符串 → 抛异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> new AsyncTxVersion(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的版本参数");
        }

        @Test
        @DisplayName("null → 抛异常")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> new AsyncTxVersion(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的版本参数");
        }

        @Test
        @DisplayName("空白字符串 → 抛异常")
        void shouldThrowForBlank() {
            assertThatThrownBy(() -> new AsyncTxVersion("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的版本参数");
        }
    }

    @Nested
    @DisplayName("compareTo()")
    class CompareTo {

        @Test
        @DisplayName("相同版本 → 0")
        void shouldReturnZeroForEqual() {
            assertThat(new AsyncTxVersion("1.0.0").compareTo(new AsyncTxVersion("1.0.0"))).isEqualTo(0);
        }

        @Test
        @DisplayName("高位更大 → 1")
        void shouldReturnPositiveWhenGreater() {
            assertThat(new AsyncTxVersion("2.0.0").compareTo(new AsyncTxVersion("1.9.9"))).isEqualTo(1);
        }

        @Test
        @DisplayName("高位更小 → -1")
        void shouldReturnNegativeWhenLess() {
            assertThat(new AsyncTxVersion("1.0.0").compareTo(new AsyncTxVersion("2.0.0"))).isEqualTo(-1);
        }

        @Test
        @DisplayName("次高位更大 → 1")
        void shouldCompareSecondSegment() {
            assertThat(new AsyncTxVersion("1.2.0").compareTo(new AsyncTxVersion("1.1.0"))).isEqualTo(1);
        }

        @Test
        @DisplayName("不同长度，短版本缺失位视为 0 → 相等")
        void shouldTreatMissingAsZero() {
            assertThat(new AsyncTxVersion("1").compareTo(new AsyncTxVersion("1.0.0"))).isEqualTo(0);
        }

        @Test
        @DisplayName("不同长度，短版本小于长版本")
        void shouldCompareDifferentLengths() {
            assertThat(new AsyncTxVersion("1.0").compareTo(new AsyncTxVersion("1.0.1"))).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("equals() / hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同 cells → equals true")
        void shouldBeEqualWithSameCells() {
            AsyncTxVersion v1 = new AsyncTxVersion("1.0.0");
            AsyncTxVersion v2 = new AsyncTxVersion("1.0.0");
            assertThat(v1).isEqualTo(v2);
            assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        }

        @Test
        @DisplayName("不同 cells → equals false")
        void shouldNotBeEqualWithDifferentCells() {
            assertThat(new AsyncTxVersion("1.0.0")).isNotEqualTo(new AsyncTxVersion("2.0.0"));
        }

        @Test
        @DisplayName("自身 → equals true")
        void shouldBeEqualToSelf() {
            AsyncTxVersion v = new AsyncTxVersion("1.0");
            assertThat(v).isEqualTo(v);
        }

        @Test
        @DisplayName("null → equals false")
        void shouldNotBeEqualToNull() {
            assertThat(new AsyncTxVersion("1.0")).isNotEqualTo(null);
        }

        @Test
        @DisplayName("不同类型 → equals false")
        void shouldNotBeEqualToOtherType() {
            assertThat(new AsyncTxVersion("1.0")).isNotEqualTo("1.0");
        }
    }
}
