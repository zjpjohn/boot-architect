package com.cloud.arch.web.support;

import java.util.Collections;
import java.util.Set;

/**
 * @param authorized  是否校验授权通过
 * @param roles       校验通过满足的角色集合
 * @param authorities 校验通过满足的权限集合
 */
public record GrantedResult(boolean authorized, Set<String> roles, Set<String> authorities) {

    /**
     * 构造角色校验结果
     *
     * @param authorized 校验结果
     * @param roles      校验通过的角色集合
     */
    public static GrantedResult ofRole(boolean authorized, Set<String> roles) {
        return new GrantedResult(authorized, roles, Collections.emptySet());
    }

    /**
     * 构造权限校验结果
     *
     * @param authorized  校验结果
     * @param authorities 校验通过的权限集合
     */
    public static GrantedResult ofAuthority(boolean authorized, Set<String> authorities) {
        return new GrantedResult(authorized, Collections.emptySet(), authorities);
    }

}
