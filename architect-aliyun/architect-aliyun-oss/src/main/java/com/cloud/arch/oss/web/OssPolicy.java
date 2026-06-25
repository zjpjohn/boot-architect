package com.cloud.arch.oss.web;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OssPolicy {
    /**
     * oss接口app
     */
    private String              appId;
    /**
     * 文件上传策略
     */
    private String              policy;
    /**
     * 签名
     */
    private String              signature;
    /**
     * 上传域名
     */
    private String              domain;
    /**
     * 上传对象key
     */
    private String              objKey;
    /**
     * policy过期时间
     */
    private Long                expire;
    /**
     * 上传结果回调地址
     * Base64 编码的回调配置（含 callbackUrl + callbackBody + callbackVar）
     */
    private String              callback;
    /**
     * 回调自定义变量
     */
    private Map<String, String> callbackVars;
}