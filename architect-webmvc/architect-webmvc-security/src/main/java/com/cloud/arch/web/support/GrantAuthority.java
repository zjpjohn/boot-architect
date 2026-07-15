package com.cloud.arch.web.support;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.annotation.Permission;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;


/**
 * @param identity 权限用户标识
 * @param mode     权限校验模式
 * @param roles    角色限定集合
 * @param permits  权限限定集合
 */
@Slf4j
public record GrantAuthority(String identity, GrantMode mode, Set<String> roles, Set<String> permits) {

    public GrantAuthority(String identity) {
        this(identity, GrantMode.AND, Collections.emptySet(), Collections.emptySet());
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(roles) && CollectionUtils.isEmpty(permits);
    }

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
     * @param roles   用户角色集合
     * @param permits 用户权限集合
     */
    public GrantedResult decide(Set<String> roles, Set<String> permits) {
        Pair<Boolean, Set<String>> role   = roleCheck(roles);
        Pair<Boolean, Set<String>> permit = permitCheck(permits);
        if (mode == GrantMode.AND) {
            boolean result = role.getKey() && permit.getKey();
            return new GrantedResult(result, role.getValue(), permit.getValue());
        }
        boolean result = role.getKey() || permit.getKey();
        return new GrantedResult(result, role.getValue(), permit.getValue());
    }

    /**
     * 是否包含指定角色集合
     *
     * @param roleSet 用户角色集合
     */
    private Pair<Boolean, Set<String>> roleCheck(Set<String> roleSet) {
        if (CollectionUtils.isEmpty(roles) || containsAll(roles) || containsAll(roleSet)) {
            return Pair.of(true, Collections.emptySet());
        }
        Set<?>      target     = Objects.requireNonNullElse(roleSet, Sets.newHashSet());
        Set<String> intersects = Sets.intersection(this.roles, target);
        return Pair.of(CollectionUtils.isNotEmpty(intersects), intersects);
    }

    /**
     * 是否包含指定权限集合
     *
     * @param permitSet 用户权限集合
     */
    private Pair<Boolean, Set<String>> permitCheck(Set<String> permitSet) {
        if (CollectionUtils.isEmpty(permits) || containsAll(permits) || containsAll(permitSet)) {
            return Pair.of(true, Collections.emptySet());
        }
        Set<?>      target     = Objects.requireNonNullElse(permitSet, Sets.newHashSet());
        Set<String> intersects = Sets.intersection(this.permits, target);
        return Pair.of(CollectionUtils.isNotEmpty(intersects), intersects);
    }

    /**
     * 判断是否包含全部权限
     */
    private boolean containsAll(Set<String> values) {
        return CollectionUtils.isNotEmpty(values) && values.contains(Permission.DEFAULT_VALUE);
    }

}
