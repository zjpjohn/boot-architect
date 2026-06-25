package com.cloud.arch.oss.web;

import com.aliyun.oss.common.utils.BinaryUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;

@Slf4j
public class OssCallbackVerifier {

    private static final String AUTH_HEADER        = "Authorization";
    private static final String PUB_KEY_URL_HEADER = "x-oss-pub-key-url";

    private final HttpClient httpClient;

    public OssCallbackVerifier() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * 获取校验公钥
     */
    private String downloadPubKey(HttpServletRequest request) throws IOException, InterruptedException {
        byte[] pubKey     = BinaryUtil.fromBase64String(request.getHeader(PUB_KEY_URL_HEADER));
        String pubKeyAddr = new String(pubKey);
        if (!pubKeyAddr.startsWith("http://gosspublic.alicdn.com/") &&
            !pubKeyAddr.startsWith("https://gosspublic.alicdn.com/")) {
            throw new IllegalArgumentException("pub key address must be oss address");
        }
        HttpRequest req = HttpRequest.newBuilder()
                                     .uri(URI.create(pubKeyAddr))
                                     .timeout(Duration.ofSeconds(5))
                                     .GET()
                                     .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("download oss public key error.");
        }
        return resp.body().replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
    }


    /**
     * 回调校验
     *
     * @param content   回调内容
     * @param sign      签名信息
     * @param publicKey 公钥密码
     */
    private boolean checkAuth(String content, byte[] sign, String publicKey) {
        try {
            KeyFactory              keyFactory = KeyFactory.getInstance("RSA");
            byte[]                  encodedKey = BinaryUtil.fromBase64String(publicKey);
            PublicKey               pubKey     = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature  = java.security.Signature.getInstance("MD5withRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(sign);
        } catch (Exception e) {
            log.error("校验直传回调内容异常:", e);
        }
        return false;
    }

    /**
     * 校验 OSS 回调请求签名，校验失败抛出 CALLBACK_SIGN_INVALID 异常。
     *
     * @param request 经过 ContentCachingRequestWrapper 包装的请求
     */
    public boolean verify(HttpServletRequest request, String body) throws Exception {
        String publicKey     = downloadPubKey(request);
        String stringToSign  = buildStringToSign(request, body);
        byte[] authorization = BinaryUtil.fromBase64String(request.getHeader(AUTH_HEADER));
        return checkAuth(stringToSign, authorization, publicKey);
    }

    /**
     * 构造待签名字符串，格式：url_decode(path) + "?" + query_string + "\n" + body
     */
    private String buildStringToSign(HttpServletRequest request, String body) {
        String path  = request.getRequestURI();
        String query = request.getQueryString();
        if (StringUtils.isNotBlank(query)) {
            path += "?" + query;
        }
        return path + "\n" + body;
    }

}