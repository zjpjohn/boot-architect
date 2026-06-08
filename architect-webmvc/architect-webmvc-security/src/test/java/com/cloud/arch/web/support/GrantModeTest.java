package com.cloud.arch.web.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GrantMode 授权模式")
class GrantModeTest {

    @Nested
    @DisplayName("of() 转换")
    class Of {

        @Test
        @DisplayName("\"OR\" / \"or\" → OR")
        void shouldMapOr() {
            assertThat(GrantMode.of("OR")).isEqualTo(GrantMode.OR);
            assertThat(GrantMode.of("or")).isEqualTo(GrantMode.OR);
        }

        @Test
        @DisplayName("\"AND\" / \"and\" → AND")
        void shouldMapAnd() {
            assertThat(GrantMode.of("AND")).isEqualTo(GrantMode.AND);
            assertThat(GrantMode.of("and")).isEqualTo(GrantMode.AND);
        }

        @Test
        @DisplayName("非法值 → 抛出异常")
        void shouldThrowForInvalidValue() {
            assertThatThrownBy(() -> GrantMode.of("XOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("XOR");
        }
    }
}
