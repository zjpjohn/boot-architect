package com.cloud.arch.oss.web;

import com.aliyun.oss.common.utils.BinaryUtil;
import com.cloud.arch.oss.props.OssCloudProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
public class UploadCallbackExecutor {

    private final OssCloudProperties properties;

    public UploadCallbackExecutor(OssCloudProperties properties) {
        this.properties = properties;
    }

    /**
     * 回调内容处理
     *
     * @param request 回调请求
     * @param body    回调内容
     */
    public String execute(HttpServletRequest request, String body) throws Exception {
        String publicKey   = getPublicKeyFromOss(request);
        String queryString = request.getQueryString();
        String authStr = URLDecoder.decode(properties.getWebDirect().getPrefix() + request.getRequestURI(), "UTF-8");
        if (StringUtils.isNotBlank(queryString)) {
            authStr += "?" + queryString;
        }
        authStr += "\n" + body;
        byte[] authorization = BinaryUtil.fromBase64String(request.getHeader("Authorization"));
        if (!checkAuth(authStr, authorization, publicKey)) {
            throw new IllegalArgumentException("直传回调校验失败");
        }
        return body;
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
     * 获取校验公钥
     */
    private String getPublicKeyFromOss(HttpServletRequest request) throws Exception {
        BufferedReader reader = null;
        try {
            byte[] pubKey     = BinaryUtil.fromBase64String(request.getHeader("x-oss-pub-key-url"));
            String pubKeyAddr = new String(pubKey);
            if (!pubKeyAddr.startsWith("http://gosspublic.alicdn.com/")
                && !pubKeyAddr.startsWith("https://gosspublic.alicdn.com/")) {
                throw new IllegalArgumentException("pub key address must be oss address");
            }
            CloseableHttpClient client   = HttpClientBuilder.create().build();
            HttpGet             httpGet  = new HttpGet(pubKeyAddr);
            HttpResponse        response = client.execute(httpGet);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder sb   = new StringBuilder();
            String        line = "", separator = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(separator);
            }
            return sb.toString().replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public OssCloudProperties getProperties() {
        return properties;
    }
}
