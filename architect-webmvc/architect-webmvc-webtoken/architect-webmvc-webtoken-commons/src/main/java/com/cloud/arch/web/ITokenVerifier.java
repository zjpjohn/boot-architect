package com.cloud.arch.web;

public interface ITokenVerifier {

    /**
     * 校验token并返回token字段信息
     *
     * @param token jwt token数据
     */
    VerifyResult verify(String token);

    /**
     * auth token header名称
     */
    default String header() {
        return WebTokenConstants.AUTH_TOKEN_HEADER;
    }

}
