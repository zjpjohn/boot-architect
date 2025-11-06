package com.cloud.token.security;

import com.cloud.token.utils.TokenConstants;

import java.util.Collections;
import java.util.Set;

public interface SecurityPrincipal<T> {

    /**
     * 多账户体系下，不同账户领域标识
     */
    default String realm() {
        return TokenConstants.DEFAULT_REALM;
    }

    /**
     * 根据账户标识查询权限集合
     */
    default Set<String> permits(T loginId) {
        return Collections.emptySet();
    }

    /**
     * 根据账户标识查询角色集合
     */
    default Set<String> roles(T loginId) {
        return Collections.emptySet();
    }

}
