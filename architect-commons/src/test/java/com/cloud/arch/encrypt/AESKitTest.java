package com.cloud.arch.encrypt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AESKit 加解密")
class AESKitTest {

    private static final String KEY = "Test@2024Key!abcdefghijk"; // 24 字节
    private static final String IV = "TestIV@2024!abcd";          // 16 字节

    @Nested
    @DisplayName("ECB 模式")
    class Ecb {

        @Test
        @DisplayName("PKCS5 加密解密往返")
        void shouldRoundTripPkc5() {
            String plainText = "Hello World 你好世界";
            String encrypted = AESKit.ECB.pkc5Enc(plainText, KEY);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.ECB.pkc5Dec(encrypted, KEY);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("PKCS7 加密解密往返")
        void shouldRoundTripPkc7() {
            String plainText = "Hello World AES PKCS7";
            String encrypted = AESKit.ECB.pkc7Enc(plainText, KEY);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.ECB.pkc7Dec(encrypted, KEY);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("ZeroPadding 加密解密往返")
        void shouldRoundTripZero() {
            String plainText = "Hello World 中文测试";
            String encrypted = AESKit.ECB.zeroEnc(plainText, KEY);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.ECB.zeroDec(encrypted, KEY);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("不同密钥产生不同密文")
        void shouldProduceDifferentCiphertextsWithDifferentKeys() {
            String anotherKey = AESKit.genKey();
            String plainText = "same text";

            String enc1 = AESKit.ECB.pkc5Enc(plainText, KEY);
            String enc2 = AESKit.ECB.pkc5Enc(plainText, anotherKey);

            assertThat(enc1).isNotEqualTo(enc2);
        }
    }

    @Nested
    @DisplayName("CBC 模式")
    class Cbc {

        @Test
        @DisplayName("PKCS5 加密解密往返")
        void shouldRoundTripPkc5() {
            String plainText = "Hello CBC 你好";
            String encrypted = AESKit.CBC.pkc5Enc(plainText, KEY, IV);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.CBC.pkc5Dec(encrypted, KEY, IV);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("PKCS7 加密解密往返")
        void shouldRoundTripPkc7() {
            String plainText = "Hello CBC PKCS7 世界";
            String encrypted = AESKit.CBC.pkc7Enc(plainText, KEY, IV);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.CBC.pkc7Dec(encrypted, KEY, IV);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("ZeroPadding 加密解密往返")
        void shouldRoundTripZero() {
            String plainText = "Zero CBC test 中文";
            String encrypted = AESKit.CBC.zeroEnc(plainText, KEY, IV);
            assertThat(encrypted).isNotBlank().isNotEqualTo(plainText);

            String decrypted = AESKit.CBC.zeroDec(encrypted, KEY, IV);
            assertThat(decrypted).isEqualTo(plainText);
        }
    }

    @Nested
    @DisplayName("密钥与向量生成")
    class KeyGen {

        @Test
        @DisplayName("genKey 返回 24 位字符串")
        void shouldGenerate24CharKey() {
            String key = AESKit.genKey();
            assertThat(key).hasSize(24);
        }

        @Test
        @DisplayName("genIv 返回 16 位字符串")
        void shouldGenerate16CharIv() {
            String iv = AESKit.genIv();
            assertThat(iv).hasSize(16);
        }

        @Test
        @DisplayName("每次 genKey 生成不同的密钥")
        void shouldGenerateUniqueKeys() {
            String k1 = AESKit.genKey();
            String k2 = AESKit.genKey();
            assertThat(k1).isNotEqualTo(k2);
        }

        @Test
        @DisplayName("生成的 key 和 iv 可用于加解密")
        void shouldEncryptAndDecryptWithGeneratedKeyAndIv() {
            String key    = AESKit.genKey();
            String iv     = AESKit.genIv();
            String plain  = "test with generated key/iv";

            String encrypted = AESKit.CBC.pkc5Enc(plain, key, iv);
            String decrypted = AESKit.CBC.pkc5Dec(encrypted, key, iv);

            assertThat(decrypted).isEqualTo(plain);
        }
    }
}
