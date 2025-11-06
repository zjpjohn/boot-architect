package com.cloud.arch.web.support;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.annotation.Permission;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;


/**
 * @param identity 权限用户标识
 * @param mode     权限校验模式
 * @param roles    角色限定集合
 * @param permits  权限限定集合
 */
@Slf4j
public record GrantAuthority(String identity, GrantMode mode, Set<String> roles, Set<String> permits) {

    /**
     * 用户角色权限校验
     */
    public GrantedResult decide(SecurityPrincipal principal) {
        GrantedPrincipal granted = principal.principal(identity);
        return this.decide(granted.roles(), granted.permits());
    }

    /**
     * 校验角色权限
     *
     * @param roles       用户角色集合
     * @param authorities 用户权限集合
     */
    public GrantedResult decide(Set<String> roles, Set<String> authorities) {
        Pair<Boolean, Set<String>> role   = roleCheck(roles);
        Pair<Boolean, Set<String>> permit = permitCheck(authorities);
        if (mode == GrantMode.AND) {
            boolean result = role.getKey() && permit.getKey();
            return new GrantedResult(result, role.getValue(), permit.getValue());
        }
        boolean result = role.getKey() || permit.getKey();
        return new GrantedResult(result, role.getValue(), permit.getValue());
    }

    /**
     * 懒加载获取数据集
     */
    private Set<String> lazyLoad(Set<String> source, Function<String, Set<String>> supplier) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptySet();
        }
        return supplier.apply(this.identity);
    }

    /**
     * 是否包含指定角色集合
     *
     * @param roleSet 用户角色集合
     */
    private Pair<Boolean, Set<String>> roleCheck(Set<String> roleSet) {
        if (CollectionUtils.isEmpty(roles) || roles.contains(Permission.DEFAULT_VALUE)) {
            return Pair.of(true, Collections.emptySet());
        }
        Set<String> intersects = Sets.intersection(this.roles, roleSet);
        return Pair.of(CollectionUtils.isNotEmpty(intersects), intersects);
    }

    /**
     * 是否包含指定权限集合
     *
     * @param permitSet 用户权限集合
     */
    public Pair<Boolean, Set<String>> permitCheck(Set<String> permitSet) {
        if (CollectionUtils.isEmpty(permits) || permits.contains(Permission.DEFAULT_VALUE)) {
            return Pair.of(true, Collections.emptySet());
        }
        Set<String> intersects = Sets.intersection(this.permits, permitSet);
        return Pair.of(CollectionUtils.isNotEmpty(intersects), intersects);
    }

}
