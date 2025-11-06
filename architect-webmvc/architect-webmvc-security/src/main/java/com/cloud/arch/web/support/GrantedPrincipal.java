package com.cloud.arch.web.support;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * 用户授权信息
 *
 * @param roles   授权角色集合
 * @param permits 授权权限集合
 */
public record GrantedPrincipal(Set<String> roles, Set<String> permits) {

    /**
     * 仅角色集合
     */
    public static GrantedPrincipal ofRoles(Set<String> roles) {
        return new GrantedPrincipal(roles, Sets.newHashSet());
    }

    /**
     * 仅权限集合
     */
    public static GrantedPrincipal ofPermits(Set<String> permits) {
        return new GrantedPrincipal(Sets.newHashSet(), permits);
    }

    /**
     * 空权角色权限集合
     */
    public static GrantedPrincipal empty() {
        return new GrantedPrincipal(Sets.newHashSet(), Sets.newHashSet());
    }

}
