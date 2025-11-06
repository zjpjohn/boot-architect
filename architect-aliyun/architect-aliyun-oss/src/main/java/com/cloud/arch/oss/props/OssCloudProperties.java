package com.cloud.arch.oss.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.oss")
public class OssCloudProperties {

    /**
     * oss接口appId
     */
    private String              appId;
    /**
     * oss接口密钥
     */
    private String              secret;
    /**
     * oss接口endpoint
     */
    private String              endpoint;
    /**
     * oss接口bucket
     */
    private String              bucket;
    /**
     * oss请求AliYun直接地址:https(http)://bucket.endpoint
     */
    private String              hostUri;
    /**
     * oss绑定域名地址:https(http)://domain
     */
    private String              domainUri;
    /**
     * web直传配置
     */
    private WebDirectProperties webDirect;

    @Data
    public static class WebDirectProperties {
        /**
         * policy过期时间
         */
        private Long   expire = 7200L;
        /**
         * 回调请求前缀地址(主要适配网关前缀地址)
         */
        private String prefix;
        /**
         * oss上传请求回调地址
         */
        private String callback;
    }

}
