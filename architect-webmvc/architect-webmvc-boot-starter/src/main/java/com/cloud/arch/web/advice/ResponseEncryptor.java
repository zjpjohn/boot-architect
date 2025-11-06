package com.cloud.arch.web.advice;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.cloud.arch.encrypt.AESKit;
import com.cloud.arch.web.domain.BodyData;
import com.cloud.arch.web.props.WebmvcProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ResponseEncryptor {

    public static final String DEFAULT_MODE    = "CBC";
    public static final String DEFAULT_PADDING = "PKCS7";
    public static final String DEFAULT_HEADER  = "web-enc-ivr";

    private ResponseEncryptor() {
    }

    /**
     * 加密body体中数据
     */
    public static BodyData<Object> encrypt(WebmvcProperties properties,
                                           Object data,
                                           String message,
                                           ServerHttpResponse response) {
        if (data == null) {
            return new BodyData<>(message, HttpStatus.OK.value(), null);
        }
        String                         body    = convertBody(data);
        WebmvcProperties.EncryptConfig encrypt = properties.getEncrypt();
        String                         mode    = encrypt.getMode().toLowerCase();
        if (DEFAULT_MODE.equalsIgnoreCase(mode)) {
            return encryptCbc(encrypt.getPadding(), encrypt.getHeader(), body, message, response);
        }
        return encryptEcb(encrypt.getPadding(), encrypt.getHeader(), body, message, response);
    }

    /**
     * CBC加密模式加密
     */
    private static BodyData<Object> encryptCbc(String padding,
                                               String header,
                                               String body,
                                               String message,
                                               ServerHttpResponse response) {
        String key         = AESKit.genKey();
        String ivr         = AESKit.genIv();
        String headerValue = encryptHeader(key, ivr);
        response.getHeaders().set(header, headerValue);
        String type = padding.toLowerCase();
        if (DEFAULT_PADDING.equalsIgnoreCase(type)) {
            String encrypt = AESKit.CBC.pkc7Enc(body, key, ivr);
            return new BodyData<>(message, HttpStatus.OK.value(), encrypt);
        }
        String encrypt = AESKit.CBC.zeroEnc(body, key, ivr);
        return new BodyData<>(message, HttpStatus.OK.value(), encrypt);
    }

    /**
     * ECB加密模式加密
     */
    private static BodyData<Object> encryptEcb(String padding,
                                               String header,
                                               String body,
                                               String message,
                                               ServerHttpResponse response) {
        String key         = AESKit.genKey();
        String headerValue = encryptHeader(key);
        response.getHeaders().set(header, headerValue);
        String type = padding.toLowerCase();
        if (DEFAULT_PADDING.equalsIgnoreCase(type)) {
            String encrypt = AESKit.ECB.pkc7Enc(body, key);
            return new BodyData<>(message, HttpStatus.OK.value(), encrypt);
        }
        String encrypt = AESKit.ECB.zeroEnc(body, key);
        return new BodyData<>(message, HttpStatus.OK.value(), encrypt);
    }

    /**
     * 响应数据转换 基本类型直接转换成string，对象转换换成json
     *
     * @param data 响应数据
     */
    private static String convertBody(Object data) {
        if (data instanceof Short
                || data instanceof Integer
                || data instanceof Long
                || data instanceof Double
                || data instanceof Float
                || data instanceof BigInteger
                || data instanceof BigDecimal) {
            return data.toString();
        }
        if (data instanceof String value) {
            return value;
        }
        return JSON.toJSONString(data, JSONWriter.Feature.WriteLongAsString, JSONWriter.Feature.BrowserCompatible);
    }

    /**
     * 构造加密header
     */
    private static String encryptHeader(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(keyBytes);
    }

    /**
     * 构造加密header
     */
    private static String encryptHeader(String key, String ivr) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] ivrBytes = ivr.getBytes(StandardCharsets.UTF_8);
        byte[] content  = new byte[keyBytes.length + ivrBytes.length];
        System.arraycopy(keyBytes, 0, content, 0, keyBytes.length);
        System.arraycopy(ivrBytes, 0, content, keyBytes.length, ivrBytes.length);
        return Base64.encodeBase64String(content);
    }

}
