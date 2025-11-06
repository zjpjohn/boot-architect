package com.cloud.arch.web;

import com.cloud.arch.enums.Value;

import java.util.Map;

public interface IHttpAuthSource extends Value<String> {

    /**
     * 默认授权域
     */
    String DEFAULT_DOMAIN = "system";

    /**
     * 请求域标识
     * 系统默认授权域-system
     */
    @Override
    default String value() {
        return DEFAULT_DOMAIN;
    }

    /**
     * 授权身份标识字段
     */
    String authKey();

    /**
     * 不同授权渠道payload个性化字段解析
     *
     * @param payload token数据载体
     */
    VerifyResult parse(Map<String, Object> payload);
}
