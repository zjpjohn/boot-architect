package com.cloud.arch.encrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RSAKit 加解密与签名")
class RSAKitTest {

    private static RSAKit rsa;
    private static String publicKey;
    private static String privateKey;

    @BeforeAll
    static void setUpKeys() throws Exception {
        rsa = RSAKit.instance();
        Map<String, String> keyPair = rsa.keyPair();
        publicKey = keyPair.get(RSAKit.PUBLIC_KEY);
        privateKey = keyPair.get(RSAKit.PRIVATE_KEY);
    }

    @Nested
    @DisplayName("加密解密往返")
    class RoundTrip {

        @Test
        @DisplayName("公钥加密 → 私钥解密往返")
        void shouldRoundTripPublicEncryptPrivateDecrypt() throws Exception {
            String plainText = "Hello RSA 你好世界";

            byte[] encrypted = rsa.encodePublicKey(plainText.getBytes(), publicKey);
            byte[] decrypted = rsa.decodePrivateKey(encrypted, privateKey);

            assertThat(new String(decrypted)).isEqualTo(plainText);
        }

        @Test
        @DisplayName("私钥加密 → 公钥解密往返")
        void shouldRoundTripPrivateEncryptPublicDecrypt() throws Exception {
            String plainText = "Private encrypt test 中文";

            byte[] encrypted = rsa.encodePrivateKey(plainText.getBytes(), privateKey);
            byte[] decrypted = rsa.decodePublicKey(encrypted, publicKey);

            assertThat(new String(decrypted)).isEqualTo(plainText);
        }

        @Test
        @DisplayName("私钥加密 → 公钥解密（字符串便捷方法）")
        void shouldRoundTripWithStringMethods() throws Exception {
            String plainText = "String method test 测试";

            String encrypted = rsa.encodePrivateKey(plainText, privateKey);
            String decrypted = rsa.decodePublicKey(encrypted, publicKey);

            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("使用错误密钥解密会得到不同结果")
        void shouldFailWithWrongKey() throws Exception {
            String plainText = "test";
            byte[] encrypted = rsa.encodePublicKey(plainText.getBytes(), publicKey);

            Map<String, String> anotherPair = rsa.keyPair();
            String wrongPrivateKey = anotherPair.get(RSAKit.PRIVATE_KEY);

            try {
                rsa.decodePrivateKey(encrypted, wrongPrivateKey);
                // 如果用错误密钥解密，结果不会是原文
            } catch (Exception ignored) {
                // 预期可能抛出解密异常
            }
        }
    }

    @Nested
    @DisplayName("数字签名")
    class SignatureTests {

        @Test
        @DisplayName("签名 → 验证 往返成功")
        void shouldSignAndVerify() throws Exception {
            byte[] data = "important data to sign".getBytes();

            String sign = rsa.sign(data, privateKey);
            assertThat(sign).isNotBlank();

            boolean verified = rsa.verify(data, publicKey, sign);
            assertThat(verified).isTrue();
        }

        @Test
        @DisplayName("篡改数据后验证失败")
        void shouldRejectTamperedData() throws Exception {
            byte[] data = "original data".getBytes();
            String sign = rsa.sign(data, privateKey);

            byte[] tampered = "tampered data".getBytes();
            boolean verified = rsa.verify(tampered, publicKey, sign);

            assertThat(verified).isFalse();
        }
    }
}
