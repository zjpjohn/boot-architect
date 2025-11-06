package com.cloud.arch.oss.web;

import lombok.Data;

@Data
public class OssPolicy {
    /**
     * oss接口app
     */
    private String appId;
    /**
     * 文件上传策略
     */
    private String policy;
    /**
     * 签名
     */
    private String signature;
    /**
     * 上传域名
     */
    private String domain;
    /**
     * 允许的文件件
     */
    private String dir;
    /**
     * 上传host地址
     */
    private String host;
    /**
     * policy过期时间
     */
    private Long   expire;
    /**
     * 上传结果回调地址
     */
    private String callback;
}
