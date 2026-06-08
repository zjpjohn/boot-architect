package com.cloud.token.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@DisplayName("CommonUtils Token 通用工具")
class CommonUtilsTest {

    @Nested
    @DisplayName("expireAt()")
    class ExpireAt {

        @Test
        @DisplayName("正常超时 → 当前时间 + timeout 毫秒")
        void shouldCalcExpireAt() {
            long before = System.currentTimeMillis();
            long result = CommonUtils.expireAt(10, TimeUnit.SECONDS);
            long after = System.currentTimeMillis();
            assertThat(result).isBetween(before + 9000, after + 11000);
        }

        @Test
        @DisplayName("timeout = NEVER_EXPIRE(-1) → 返回 NEVER_EXPIRE")
        void shouldReturnNeverExpireForNegativeOne() {
            long result = CommonUtils.expireAt(-1, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(TokenConstants.NEVER_EXPIRE);
        }

        @Test
        @DisplayName("timeout < NEVER_EXPIRE → 返回 NEVER_EXPIRE")
        void shouldReturnNeverExpireForLessThanNegativeOne() {
            long result = CommonUtils.expireAt(-99, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(TokenConstants.NEVER_EXPIRE);
        }

        @Test
        @DisplayName("timeout = 0 → 返回当前时间")
        void shouldReturnCurrentTimeForZero() {
            long before = System.currentTimeMillis();
            long result = CommonUtils.expireAt(0, TimeUnit.SECONDS);
            assertThat(result).isBetween(before - 100, before + 100);
        }

        @Test
        @DisplayName("TimeUnit.MINUTES → 正确转换")
        void shouldConvertMinutesToMillis() {
            long before = System.currentTimeMillis();
            long result = CommonUtils.expireAt(1, TimeUnit.MINUTES);
            assertThat(result).isBetween(before + 59000, before + 61000);
        }
    }

    @Nested
    @DisplayName("key()")
    class Key {

        @Test
        @DisplayName("prefix:key 格式")
        void shouldBuildKeyWithPrefix() {
            assertThat(CommonUtils.key("t", "abc123")).isEqualTo("t:abc123");
        }

        @Test
        @DisplayName("数字 key → prefix:数字")
        void shouldBuildKeyWithNumericKey() {
            assertThat(CommonUtils.key("t", 123)).isEqualTo("t:123");
        }
    }

    @Nested
    @DisplayName("sessionId()")
    class SessionId {

        @Test
        @DisplayName("有 realm → prefix:realm:key")
        void shouldBuildWithRealm() {
            assertThat(CommonUtils.sessionId("s", "myRealm", "user1")).isEqualTo("s:myRealm:user1");
        }

        @Test
        @DisplayName("realm 为空 → prefix:key")
        void shouldBuildWithEmptyRealm() {
            assertThat(CommonUtils.sessionId("s", "", "user1")).isEqualTo("s:user1");
        }

        @Test
        @DisplayName("realm 为 null → prefix:key")
        void shouldBuildWithNullRealm() {
            assertThat(CommonUtils.sessionId("s", null, "user1")).isEqualTo("s:user1");
        }
    }
}
