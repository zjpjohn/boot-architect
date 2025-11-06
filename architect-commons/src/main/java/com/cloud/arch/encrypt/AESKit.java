package com.cloud.arch.encrypt;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.Random;

public class AESKit {

    private static final String       UNSUPPORTED      = "not support invoke.";
    private static final int          KEY_LENGTH       = 24;
    private static final int          IV_LENGTH        = 16;
    private static final String       KEY_ALGORITHM    = "AES";
    private static final Random       RANDOM           = new Random();
    private static final List<String> ALPHA_COLLECTION = List.of("0123456789", "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "!@#$%&?*+");

    private AESKit() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    /**
     * ECB加解密模式
     */
    public static class ECB {

        static final String ECB_PKC5S_PADDING = "AES/ECB/PKCS5Padding";
        static final String ECB_PKC7S_PADDING = "AES/ECB/PKCS7Padding";
        static final String ECB_ZERO_PADDING  = "AES/ECB/NoPadding";

        static {
            if (Security.getProperty(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }

        private ECB() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * PKC5SPadding加密模式
         *
         * @param source 加密字符串
         * @param key    加密密钥
         */
        public static String pkc5Enc(String source, String key) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, ECB_PKC5S_PADDING, null);
        }

        /**
         * PKC5SPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密钥
         */
        public static String pkc5Dec(String source, String key) {
            return decrypt(source, key, ECB_PKC5S_PADDING, null);
        }

        /**
         * PKC7SPadding加密模式
         *
         * @param source 加密字符串
         * @param key    加密密钥
         */
        public static String pkc7Enc(String source, String key) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, ECB_PKC7S_PADDING, "BC");
        }

        /**
         * PK7SPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密钥
         */
        public static String pkc7Dec(String source, String key) {
            return decrypt(source, key, ECB_PKC7S_PADDING, "BC");
        }

        /**
         * NoPadding加密模式
         *
         * @param source 加密字符串
         * @param key    加密密钥
         */
        public static String zeroEnc(String source, String key) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, ECB_ZERO_PADDING, null);
        }

        /**
         * NoPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密钥
         */
        public static String zeroDec(String source, String key) {
            return decrypt(source, key, ECB_ZERO_PADDING, null);
        }

        /**
         * ecb加密通用处理
         *
         * @param source 待加密字符串字节码
         * @param key    加密密码
         * @param mode   加密padding模式
         * @param name   加密provider名称
         */
        private static String encrypt(byte[] source, String key, String mode, String name) {
            try {
                Cipher        cipher  = getInstance(mode, name);
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                if (ECB_ZERO_PADDING.equals(mode)) {
                    source = zeroPadding(source, cipher.getBlockSize());
                }
                byte[] bytes = cipher.doFinal(source);
                return Base64.encodeBase64String(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        /**
         * ecb机密通用处理
         *
         * @param source 待解密字符串
         * @param key    解密密码
         * @param mode   解密padding欧式
         * @param name   解密provider名称
         */
        public static String decrypt(String source, String key, String mode, String name) {
            try {
                Cipher        cipher  = getInstance(mode, name);
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] bytes = cipher.doFinal(Base64.decodeBase64(source));
                if (ECB_ZERO_PADDING.equals(mode)) {
                    bytes = erasePadding(bytes, cipher.getBlockSize());
                }
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * CBC加解密模式
     */
    public static class CBC {

        static final String CBC_PKC5S_PADDING = "AES/CBC/PKCS5Padding";
        static final String CBC_PKC7S_PADDING = "AES/CBC/PKCS7Padding";
        static final String CBC_ZERO_PADDING  = "AES/CBC/NoPadding";

        static {
            if (Security.getProperty(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }

        private CBC() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * PKC5SPadding模式加密
         *
         * @param source 加密字符串
         * @param key    加密密钥
         * @param iv     加密偏移向量
         */
        public static String pkc5Enc(String source, String key, String iv) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, iv, CBC_PKC5S_PADDING, null);
        }

        /**
         * PKC5SPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密码
         * @param iv     解密偏移向量
         */
        public static String pkc5Dec(String source, String key, String iv) {
            return decrypt(source, key, iv, CBC_PKC5S_PADDING, null);
        }

        /**
         * PKC7SPadding加密模式
         *
         * @param source 加密字符串
         * @param key    加密秘钥
         * @param iv     加密偏移向量
         */
        public static String pkc7Enc(String source, String key, String iv) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, iv, CBC_PKC7S_PADDING, "BC");
        }

        /**
         * PKC7SPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密钥
         * @param iv     解密偏移向量
         */
        public static String pkc7Dec(String source, String key, String iv) {
            return decrypt(source, key, iv, CBC_PKC7S_PADDING, "BC");
        }

        /**
         * NoPadding加密模式
         *
         * @param source 加密字符串
         * @param key    加密密钥
         * @param iv     加密偏移向量
         */
        public static String zeroEnc(String source, String key, String iv) {
            return encrypt(source.getBytes(StandardCharsets.UTF_8), key, iv, CBC_ZERO_PADDING, null);
        }

        /**
         * NoPadding解密模式
         *
         * @param source 解密字符串
         * @param key    解密密钥
         * @param iv     解密偏移向量
         */
        public static String zeroDec(String source, String key, String iv) {
            return decrypt(source, key, iv, CBC_ZERO_PADDING, null);
        }

        /**
         * cbc通用加密处理
         *
         * @param source 原字符串
         * @param key    加密密码
         * @param iv     加密偏移量
         * @param mode   加密padding模式
         * @param name   provider名称
         */
        private static String encrypt(byte[] source, String key, String iv, String mode, String name) {
            try {
                Cipher          cipher  = getInstance(mode, name);
                SecretKeySpec   keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
                IvParameterSpec ivSpec  = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                if (CBC_ZERO_PADDING.equals(mode)) {
                    source = zeroPadding(source, cipher.getBlockSize());
                }
                byte[] bytes = cipher.doFinal(source);
                return Base64.encodeBase64String(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        /**
         * cbc通用解密处理
         *
         * @param source 待解密字符串
         * @param key    解密密码
         * @param iv     解密偏移量
         * @param mode   解密padding模式
         * @param name   解密provider名称
         */
        public static String decrypt(String source, String key, String iv, String mode, String name) {
            try {
                Cipher          cipher  = getInstance(mode, name);
                SecretKeySpec   keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
                IvParameterSpec ivSpec  = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                byte[] bytes = cipher.doFinal(Base64.decodeBase64(source));
                if (CBC_ZERO_PADDING.equals(mode)) {
                    bytes = erasePadding(bytes, cipher.getBlockSize());
                }
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * 提供生成长度为24的字符串密码
     */
    public static String genKey() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < KEY_LENGTH; i++) {
            String base     = ALPHA_COLLECTION.get(i % 4);
            int    position = RANDOM.nextInt(base.length());
            builder.append(base.charAt(position));
        }
        return builder.toString();
    }

    /**
     * 提供生成16位的加密偏移量
     */
    public static String genIv() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < IV_LENGTH; i++) {
            String base     = ALPHA_COLLECTION.get(i % 3);
            int    position = RANDOM.nextInt(base.length());
            builder.append(base.charAt(position));
        }
        return builder.toString();
    }

    /**
     * 获取cipher
     *
     * @param mode padding填充模式
     * @param name provider名称(允许为空)
     */
    private static Cipher getInstance(String mode, String name) throws Exception {
        if (StringUtils.isNotBlank(name)) {
            return Cipher.getInstance(mode, name);
        }
        return Cipher.getInstance(mode);
    }

    /**
     * 填充字符串至长度为16的倍数
     */
    private static byte[] zeroPadding(byte[] source, int blockSize) {
        int    length = source.length;
        int    remain = blockSize - length % blockSize;
        byte[] data   = new byte[length + remain];
        System.arraycopy(source, 0, data, 0, length);
        return data;
    }

    /**
     * 擦除尾部填充数据
     *
     * @param data      加密后的数据
     * @param blockSize 数据块大小
     */
    private static byte[] erasePadding(byte[] data, int blockSize) {
        int length = data.length;
        int remain = length % blockSize;
        if (remain != 0) {
            return data;
        }
        int i = length - 1;
        while (i >= 0 && data[i] == 0) {
            i--;
        }
        byte[] result = new byte[i + 1];
        System.arraycopy(data, 0, result, 0, i + 1);
        return result;
    }

}
