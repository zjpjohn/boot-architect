package com.cloud.arch.web;

import java.util.Map;

public interface ITokenCreator {

    /**
     * 根据claims生成token
     *
     * @param source 请求授权渠道
     * @param claims payload数据
     */
    TokenResult create(IHttpAuthSource source, Map<String, Object> claims);

}
