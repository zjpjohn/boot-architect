package com.cloud.arch.web;

public interface IAuthRequestExclude {

    /**
     * 授权请求排除uri
     */
    boolean isExclude(String requestUri, String method);

}
