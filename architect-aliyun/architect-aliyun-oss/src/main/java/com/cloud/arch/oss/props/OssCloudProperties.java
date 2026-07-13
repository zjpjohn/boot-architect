package com.cloud.arch.oss.props;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Date;

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

    public String hostPrefix() {
        if (StringUtils.isNotBlank(this.domainUri)) {
            return this.domainUri;
        }
        if (StringUtils.isNotBlank(hostUri)) {
            return this.hostUri;
        }
        return String.format("https://%s.%s", bucket, this.endpoint);
    }

    @Data
    public static class WebDirectProperties {
        /**
         * policy过期时间
         */
        private Long   expire = 300L;
        /**
         * oss上传请求回调地址
         */
        private String callback;

        public Date expireDate() {
            return new Date(System.currentTimeMillis() + this.expire * 1000L);
        }
    }

}
