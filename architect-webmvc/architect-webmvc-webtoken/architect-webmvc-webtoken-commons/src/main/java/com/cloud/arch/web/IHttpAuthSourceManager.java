package com.cloud.arch.web;

import java.util.Map;

public interface IHttpAuthSourceManager {

    /**
     * 添加授权域
     */
    default void addSource(IHttpAuthSource source) {
    }

    /**
     * 获取请求域集合
     */
    Map<String, IHttpAuthSource> ofList();

    /**
     * 获取指定域的请求授权域信息
     */
    IHttpAuthSource ofKey(String domain);

}
