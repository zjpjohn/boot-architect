package com.cloud.arch.web;


public abstract class WebTokenConstants {

    private WebTokenConstants() {
        throw new UnsupportedOperationException("不支持构造函数创建.");
    }

    /**
     * token签名错误提示
     */
    public static final String   TOKEN_SIGNATURE_ERROR = "token signature error.";
    /**
     * token过期错误提示
     */
    public static final String   TOKEN_EXPIRED_ERROR   = "token has expired.";
    /**
     * token不合规错误提示
     */
    public static final String   TOKEN_ILLEGAL_ERROR   = "token is illegal.";
    /**
     * token校验错误提示
     */
    public static final String   TOKEN_VERIFIED_ERROR  = "token checked error.";
    /**
     * token无效提示
     */
    public static final String   TOKEN_INVALID_ERROR   = "invalid token.";
    /**
     * 登录错误提示消息
     */
    public static final String   LOGIN_ERROR_MESSAGE   = "login error，please try again.";
    /**
     * 无权访问提示
     */
    public static final String   FORBIDDEN_MESSAGE     = "forbidden request";
    /**
     * 系统错误提示
     */
    public static final String   SERVER_EXCEPTION      = "system error,please try later";
    /**
     * 未授权提示消息
     */
    public static final String   UNAUTHORIZED_MESSAGE  = "please login first.";
    /**
     * 授权header标识
     */
    public static final String   AUTH_TOKEN_HEADER     = "authentication";
    /**
     * payload标识
     */
    public static final String   PAYLOAD_KEY           = "payload";
    /**
     * 请求渠道标识header
     */
    public static final String   ACCESS_SOURCE_HEADER  = "Auth-Access-Domain";
    /**
     * jwt中渠道claim名称
     */
    public static final String   JWT_CLAIM_SOURCE_KEY  = "domain";
    /**
     * 本次token的唯一标识
     */
    public static final String   JWT_CLAIM_TOKEN_KEY   = "tokenId";
    /**
     * 请求用户标识header
     */
    public static final String   AUTH_IDENTITY_HEADER  = "Auth-Request-Identity";
    /**
     * 网关请求filter优先级值
     */
    public static final Integer  AUTH_FILTER_ORDER     = 10;
    /**
     * 静态资源集合
     */
    public static final String[] STATIC_RESOURCES      = {
            "/**/*.html",
            "/**/*.css",
            "/**/*.js",
            "/**/*.png",
            "/**/*.jpg",
            "/**/*.jpeg",
            "/**/*.JPG",
            "/**/*.webp",
            "/**/*.ico"
    };

}
