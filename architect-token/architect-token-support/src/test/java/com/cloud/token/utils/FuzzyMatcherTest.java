package com.cloud.token.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FuzzyMatcher 模糊匹配器")
class FuzzyMatcherTest {

    @Nested
    @DisplayName("通配符模式")
    class Wildcard {

        @Test
        @DisplayName("前缀通配 */api → 匹配 /user/api")
        void shouldMatchPrefixWildcard() {
            FuzzyMatcher matcher = new FuzzyMatcher("*/api");
            assertThat(matcher.match("/user/api")).isTrue();
        }

        @Test
        @DisplayName("前缀通配 */api → 不匹配 /user/home")
        void shouldNotMatchPrefixWildcardWhenDifferent() {
            FuzzyMatcher matcher = new FuzzyMatcher("*/api");
            assertThat(matcher.match("/user/home")).isFalse();
        }

        @Test
        @DisplayName("后缀通配 /api/* → 匹配 /api/user")
        void shouldMatchSuffixWildcard() {
            FuzzyMatcher matcher = new FuzzyMatcher("/api/*");
            assertThat(matcher.match("/api/user")).isTrue();
        }

        @Test
        @DisplayName("中间通配 a*b → 匹配 axxxb")
        void shouldMatchMiddleWildcard() {
            FuzzyMatcher matcher = new FuzzyMatcher("a*b");
            assertThat(matcher.match("axxxxxb")).isTrue();
        }

        @Test
        @DisplayName("多个通配符 *test* → 匹配 xxxtestxxx")
        void shouldMatchMultipleWildcards() {
            FuzzyMatcher matcher = new FuzzyMatcher("*test*");
            assertThat(matcher.match("this_is_test_value")).isTrue();
        }

        @Test
        @DisplayName("通配符模式 → 不匹配不满足的")
        void shouldNotMatchUnmatchedWildcard() {
            FuzzyMatcher matcher = new FuzzyMatcher("admin-*");
            assertThat(matcher.match("user-login")).isFalse();
        }
    }

    @Nested
    @DisplayName("精确匹配模式（不含通配符）")
    class Exact {

        @Test
        @DisplayName("pattern 包含 target → true")
        void shouldMatchWhenPatternContainsTarget() {
            FuzzyMatcher matcher = new FuzzyMatcher("/admin/login");
            assertThat(matcher.match("admin")).isTrue();
        }

        @Test
        @DisplayName("pattern 不包含 target → false")
        void shouldNotMatchWhenPatternNotContainsTarget() {
            FuzzyMatcher matcher = new FuzzyMatcher("admin");
            assertThat(matcher.match("/user/login")).isFalse();
        }

        @Test
        @DisplayName("完全相等 → true")
        void shouldMatchWhenExactlyEqual() {
            FuzzyMatcher matcher = new FuzzyMatcher("admin");
            assertThat(matcher.match("admin")).isTrue();
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("目标为 null → false")
        void shouldReturnFalseForNullTarget() {
            FuzzyMatcher matcher = new FuzzyMatcher("*");
            assertThat(matcher.match(null)).isFalse();
        }

        @Test
        @DisplayName("目标为空字符串 → false")
        void shouldReturnFalseForEmptyTarget() {
            FuzzyMatcher matcher = new FuzzyMatcher("*");
            assertThat(matcher.match("")).isFalse();
        }

        @Test
        @DisplayName("目标为空白 → false")
        void shouldReturnFalseForBlankTarget() {
            FuzzyMatcher matcher = new FuzzyMatcher("*");
            assertThat(matcher.match("  ")).isFalse();
        }

        @Test
        @DisplayName("纯 * 通配符 → 匹配任意非空")
        void shouldMatchAnyNonBlankWithStarOnly() {
            FuzzyMatcher matcher = new FuzzyMatcher("*");
            assertThat(matcher.match("anything")).isTrue();
        }

        @Test
        @DisplayName("空 pattern → 不匹配任何非空 target")
        void shouldNotMatchNonEmptyTargetWithEmptyPattern() {
            FuzzyMatcher matcher = new FuzzyMatcher("");
            assertThat(matcher.match("abc")).isFalse(); // "".contains("abc") is false
        }

        @Test
        @DisplayName("空 pattern + 空 target → blank 返回 false")
        void shouldReturnFalseForEmptyTargetWithEmptyPattern() {
            FuzzyMatcher matcher = new FuzzyMatcher("");
            assertThat(matcher.match("")).isFalse(); // blank target
        }
    }
}
