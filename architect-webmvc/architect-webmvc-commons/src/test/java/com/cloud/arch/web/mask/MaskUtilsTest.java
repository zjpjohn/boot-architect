package com.cloud.arch.web.mask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MaskUtils 脱敏工具")
class MaskUtilsTest {

    @Nested
    @DisplayName("手机号脱敏")
    class Mobile {

        @Test
        @DisplayName("11 位手机号 → 中间 4 位替换为 ****")
        void shouldMaskMobile() {
            assertThat(MaskUtils.mask("13812345678", MaskType.MOBILE, 0, '*', 0))
                    .isEqualTo("138****5678");
        }

        @Test
        @DisplayName("不足 11 位 → 原样返回")
        void shouldNotMaskShortMobile() {
            assertThat(MaskUtils.mask("12345", MaskType.MOBILE, 0, '*', 0))
                    .isEqualTo("12345");
        }
    }

    @Nested
    @DisplayName("身份证脱敏")
    class IdCard {

        @Test
        @DisplayName("18 位身份证 → 中间 8 位替换")
        void shouldMask18DigitIdCard() {
            assertThat(MaskUtils.mask("110101199001011234", MaskType.ID_CARD, 0, '*', 0))
                    .isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("15 位身份证 → 中间 7 位替换")
        void shouldMask15DigitIdCard() {
            assertThat(MaskUtils.mask("110101900101123", MaskType.ID_CARD, 0, '*', 0))
                    .isEqualTo("1101*******1123");
        }

        @Test
        @DisplayName("非 15/18 位 → 原样返回")
        void shouldNotMaskInvalidIdCard() {
            assertThat(MaskUtils.mask("12345", MaskType.ID_CARD, 0, '*', 0))
                    .isEqualTo("12345");
        }
    }

    @Nested
    @DisplayName("邮箱脱敏")
    class Email {

        @Test
        @DisplayName("常规邮箱 → 前缀保留 2 位")
        void shouldMaskEmail() {
            assertThat(MaskUtils.mask("abc@test.com", MaskType.EMAIL, 0, '*', 0))
                    .isEqualTo("ab***@test.com");
        }

        @Test
        @DisplayName("短前缀邮箱 → 前缀全保留后追加 ***")
        void shouldMaskShortEmail() {
            assertThat(MaskUtils.mask("a@test.com", MaskType.EMAIL, 0, '*', 0))
                    .isEqualTo("a***@test.com");
        }

        @Test
        @DisplayName("无 @ → 原样返回")
        void shouldNotMaskWithoutAt() {
            assertThat(MaskUtils.mask("noemail", MaskType.EMAIL, 0, '*', 0))
                    .isEqualTo("noemail");
        }
    }

    @Nested
    @DisplayName("姓名脱敏")
    class Name {

        @Test
        @DisplayName("多字姓名 → 首字 + **")
        void shouldMaskMultiCharName() {
            assertThat(MaskUtils.mask("张三", MaskType.NAME, 0, '*', 0))
                    .isEqualTo("张**");
        }

        @Test
        @DisplayName("单字姓名 → *")
        void shouldMaskSingleCharName() {
            assertThat(MaskUtils.mask("张", MaskType.NAME, 0, '*', 0))
                    .isEqualTo("*");
        }
    }

    @Nested
    @DisplayName("密码脱敏")
    class Password {

        @Test
        @DisplayName("任意密码 → ********")
        void shouldMaskPassword() {
            assertThat(MaskUtils.mask("mySecret123", MaskType.PASSWORD, 0, '*', 0))
                    .isEqualTo("********");
        }
    }

    @Nested
    @DisplayName("银行卡脱敏")
    class BankCard {

        @Test
        @DisplayName("长于 8 位 → 保留首尾各 4 位")
        void shouldMaskBankCard() {
            assertThat(MaskUtils.mask("6222021234567890", MaskType.BANK_CARD, 0, '*', 0))
                    .isEqualTo("6222****7890");
        }

        @Test
        @DisplayName("等于 8 位 → 原样返回")
        void shouldNotMaskShortBankCard() {
            assertThat(MaskUtils.mask("12345678", MaskType.BANK_CARD, 0, '*', 0))
                    .isEqualTo("12345678");
        }
    }

    @Nested
    @DisplayName("CUSTOM 按比例脱敏")
    class Custom {

        @Test
        @DisplayName("50% 比例 10 个字符 → 中间 5 个 *")
        void shouldMaskByRatio() {
            assertThat(MaskUtils.mask("abcdefghij", MaskType.CUSTOM, 0.5, '*', 0))
                    .isEqualTo("ab*****hij");
        }
    }

    @Nested
    @DisplayName("空白处理")
    class Blank {

        @Test
        @DisplayName("null → null")
        void shouldReturnNullForNull() {
            assertThat(MaskUtils.mask(null, MaskType.CUSTOM, 0, '*', 0)).isNull();
        }

        @Test
        @DisplayName("空字符串 → 空字符串")
        void shouldReturnEmptyForEmpty() {
            assertThat(MaskUtils.mask("", MaskType.CUSTOM, 0, '*', 0)).isEmpty();
        }
    }
}
