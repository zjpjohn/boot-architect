package com.cloud.arch.web.support;


/**
 * 用户是否具备访问接口的权限
 */
public interface SecurityPrincipal {

    /**
     * 用户权限授权域,注意域授权域保持一致
     */
    String domain();

    /**
     * 获取用户授权信息
     */
    GrantedPrincipal principal(String identity);

}
