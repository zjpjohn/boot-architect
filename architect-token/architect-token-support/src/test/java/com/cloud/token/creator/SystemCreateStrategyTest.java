package com.cloud.token.creator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SystemCreateStrategy 系统 Token 创建策略")
class SystemCreateStrategyTest {

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("委托 TokenStyle.generate()")
        void shouldDelegateToTokenStyle() {
            SystemCreateStrategy strategy = new SystemCreateStrategy(TokenStyle.STYLE_SIMPLE_UUID);
            String token = strategy.create("user001", "default");
            assertThat(token).hasSize(32);
            assertThat(token).doesNotContain("-");
        }

        @Test
        @DisplayName("每次生成不同值")
        void shouldGenerateDifferentTokens() {
            SystemCreateStrategy strategy = new SystemCreateStrategy(TokenStyle.STYLE_RANDOM_32);
            String t1 = strategy.create("user1", "default");
            String t2 = strategy.create("user1", "default");
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        @DisplayName("loginId/realm 参数不参与生成（用于一致性 hash 的预留参数）")
        void shouldNotUseLoginIdOrRealmInGeneration() {
            SystemCreateStrategy strategy = new SystemCreateStrategy(TokenStyle.STYLE_UUID);
            String t1 = strategy.create("userA", "realmA");
            String t2 = strategy.create("userB", "realmB");
            // 仅验证返回格式正确，loginId/realm 不影响生成结果
            assertThat(t1).hasSize(36);
            assertThat(t2).hasSize(36);
        }
    }
}
