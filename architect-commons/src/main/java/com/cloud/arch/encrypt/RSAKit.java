package com.cloud.arch.encrypt;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RSAKit {

    //加密算法RSA
    private static final String KEY_ALGORITHM = "RSA";

    //签名算法
    private static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    // 获取公钥的key
    public static final String PUBLIC_KEY = "RSAPublicKey";

    // 获取私钥的key
    public static final String PRIVATE_KEY = "RSAPrivateKey";

    // RSA最大加密明文大小
    private static final int MAX_ENCRYPT_BLOCK = 117;

    // RSA最大解密密文大小
    private static final int MAX_DECRYPT_BLOCK = 128;

    // RSA 位数 如果采用2048 上面最大加密和最大解密则须填写:  245 256
    private static final int INITIALIZE_LENGTH = 1024;

    private static final class InstantHolder {
        static RSAKit kit = new RSAKit();
    }

    public static RSAKit instance() {
        return InstantHolder.kit;
    }

    /**
     * <p>
     * 生成密钥对(公钥和私钥)
     * </p>
     */
    public Map<String, String> genKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGen.initialize(INITIALIZE_LENGTH);
        KeyPair             keyPair    = keyPairGen.generateKeyPair();
        RSAPublicKey        publicKey  = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey       privateKey = (RSAPrivateKey)keyPair.getPrivate();
        String              pubKey     = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String              priKey     = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        Map<String, String> keyMap     = HashMap.newHashMap(2);
        keyMap.put(PUBLIC_KEY, pubKey);
        keyMap.put(PRIVATE_KEY, priKey);
        return keyMap;
    }

    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     *
     * @param data       已加密数据
     * @param privateKey 私钥(BASE64编码)
     */
    public String sign(byte[] data, String privateKey) throws Exception {
        byte[]              keyBytes     = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory          keyFactory   = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey          privateK     = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature           signature    = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * <p>
     * 校验数字签名
     * </p>
     *
     * @param data      已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @param sign      数字签名
     */
    public boolean verify(byte[] data, String publicKey, String sign) throws Exception {
        byte[]             keyBytes   = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec    = new X509EncodedKeySpec(keyBytes);
        KeyFactory         keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey          publicK    = keyFactory.generatePublic(keySpec);
        Signature          signature  = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicK);
        signature.update(data);
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    /**
     * <P>
     * 私钥解密
     * </p>
     *
     * @param encryptedData 已加密数据
     * @param privateKey    私钥(BASE64编码)
     */
    public byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey) throws Exception {
        byte[]              keyBytes     = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory          keyFactory   = KeyFactory.getInstance(KEY_ALGORITHM);
        Key                 privateK     = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher              cipher       = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateK);
        int                   inputLen = encryptedData.length;
        ByteArrayOutputStream out      = new ByteArrayOutputStream();
        int                   offSet   = 0;
        byte[]                cache;
        int                   i        = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * <p>
     * 公钥解密
     * </p>
     *
     * @param encryptedData 已加密数据
     * @param publicKey     公钥(BASE64编码)
     */
    public byte[] decryptByPublicKey(byte[] encryptedData, String publicKey) throws Exception {
        byte[]             keyBytes    = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory         keyFactory  = KeyFactory.getInstance(KEY_ALGORITHM);
        Key                publicK     = keyFactory.generatePublic(x509KeySpec);
        Cipher             cipher      = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int                   inputLen = encryptedData.length;
        ByteArrayOutputStream out      = new ByteArrayOutputStream();
        int                   offSet   = 0;
        byte[]                cache;
        int                   i        = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param data      源数据
     * @param publicKey 公钥(BASE64编码)
     */
    public byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
        byte[]             keyBytes    = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory         keyFactory  = KeyFactory.getInstance(KEY_ALGORITHM);
        Key                publicK     = keyFactory.generatePublic(x509KeySpec);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int                   inputLen = data.length;
        ByteArrayOutputStream out      = new ByteArrayOutputStream();
        int                   offSet   = 0;
        byte[]                cache;
        int                   i        = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * <p>
     * 私钥加密
     * </p>
     *
     * @param data       源数据
     * @param privateKey 私钥(BASE64编码)
     */
    public byte[] encryptByPrivateKey(byte[] data, String privateKey) throws Exception {
        byte[]              keyBytes     = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory          keyFactory   = KeyFactory.getInstance(KEY_ALGORITHM);
        Key                 privateK     = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher              cipher       = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, privateK);
        int                   inputLen = data.length;
        ByteArrayOutputStream out      = new ByteArrayOutputStream();
        int                   offSet   = 0;
        byte[]                cache;
        int                   i        = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 私钥加密内容
     *
     * @param data       待加密内容
     * @param privateKey rsa私钥
     */
    public String encryptPrivateKey(String data, String privateKey) throws Exception {
        byte[] bytes = encryptByPrivateKey(data.getBytes(), privateKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 公钥解密
     *
     * @param data      加密内容
     * @param publicKey rsa公钥
     */
    public String decryptPublicKey(String data, String publicKey) throws Exception {
        byte[] bytes   = Base64.getDecoder().decode(data);
        byte[] decrypt = decryptByPublicKey(bytes, publicKey);
        return new String(decrypt);
    }

}
