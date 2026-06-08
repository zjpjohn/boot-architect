package com.cloud.arch.idempotent.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IdempotentInfo 幂等信息")
class IdempotentInfoTest {

    @Nested
    @DisplayName("完整构造")
    class FullConstructor {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            IdempotentInfo info = new IdempotentInfo("my-key", "db01",
                    Duration.ofSeconds(30), "duplicate request", true);

            assertThat(info.key()).isEqualTo("my-key");
            assertThat(info.sharding()).isEqualTo("db01");
            assertThat(info.duration()).isEqualTo(Duration.ofSeconds(30));
            assertThat(info.message()).isEqualTo("duplicate request");
            assertThat(info.removeNow()).isTrue();
        }
    }

    @Nested
    @DisplayName("简化构造")
    class CompactConstructor {

        @Test
        @DisplayName("简化构造函数默认 sharding 为空字符串，removeNow 为 false")
        void shouldDefaultShardingAndRemoveNow() {
            IdempotentInfo info = new IdempotentInfo("key1",
                    Duration.ofSeconds(10), "too many requests");

            assertThat(info.sharding()).isEmpty();
            assertThat(info.removeNow()).isFalse();
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段的 record 相等")
        void shouldEqualWithSameFields() {
            IdempotentInfo i1 = new IdempotentInfo("k", Duration.ZERO, "msg");
            IdempotentInfo i2 = new IdempotentInfo("k", Duration.ZERO, "msg");
            assertThat(i1).isEqualTo(i2);
            assertThat(i1.hashCode()).isEqualTo(i2.hashCode());
        }

        @Test
        @DisplayName("不同 key 的 record 不相等")
        void shouldNotEqualWithDifferentKey() {
            IdempotentInfo i1 = new IdempotentInfo("k1", Duration.ZERO, "msg");
            IdempotentInfo i2 = new IdempotentInfo("k2", Duration.ZERO, "msg");
            assertThat(i1).isNotEqualTo(i2);
        }
    }
}
