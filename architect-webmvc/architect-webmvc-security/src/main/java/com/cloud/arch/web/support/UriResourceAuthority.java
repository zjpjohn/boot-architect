package com.cloud.arch.web.support;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.annotation.Permission;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class UriResourceAuthority {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final String         ROLE_PREFIX    = "role(";
    private static final String         PERMIT_PREFIX  = "permit(";
    private static final String         SUFFIX         = ")";

    /**
     * 请求资源uri模式
     */
    @Getter
    private final String      resource;
    /**
     * 请求方法,为空表示全部方法
     */
    private final Set<String> methods;
    /**
     * 角色权限校验模式
     */
    @Getter
    private final GrantMode   mode;
    /**
     * 请求资源允许的访问域集合,为空表示全部用户访问域
     */
    private final Set<String> domains = Sets.newHashSet();
    /**
     * 请求资源允许的访问角色集合,为空表示全部角色
     */
    private final Set<String> roles   = Sets.newHashSet();
    /**
     * 请求资源允许的访问权限集合，为空标识全部角色
     */
    private final Set<String> permits = Sets.newHashSet();

    public UriResourceAuthority(String resource,
                                Set<String> methods,
                                Set<String> domains,
                                GrantMode mode,
                                Set<String> permits,
                                Set<String> roles) {
        if (StringUtils.isBlank(resource)) {
            throw new IllegalArgumentException("authority resource url pattern must not be null.");
        }
        //用户访问域和用户角色权限不可全部都为空
        if (CollectionUtils.isEmpty(domains) && CollectionUtils.isEmpty(roles) && CollectionUtils.isEmpty(permits)) {
            throw new IllegalArgumentException("domains,roles,authorities at least one not be null.");
        }
        this.mode     = mode;
        this.resource = resource;
        this.methods  = methods;
        if (CollectionUtils.isNotEmpty(domains)) {
            this.domains.addAll(domains);
        }
        if (CollectionUtils.isNotEmpty(roles)) {
            this.roles.addAll(roles);
        }
        if (CollectionUtils.isNotEmpty(permits)) {
            this.permits.addAll(permits);
        }
    }

    /**
     * 资源匹配计算
     *
     * @param requestUri 请求uri
     * @param method     请求方法
     */
    public boolean match(String requestUri, String method) {
        return (CollectionUtils.isEmpty(this.methods) || this.methods.contains(method.toLowerCase()))
                && antPathMatcher.match(this.resource, requestUri);
    }

    public Set<String> getMethods() {
        return Collections.unmodifiableSet(methods);
    }

    public Set<String> getDomains() {
        return Collections.unmodifiableSet(domains);
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Set<String> getPermits() {
        return Collections.unmodifiableSet(permits);
    }

    public static UriResourceAuthority parse(String target) {
        String[] splits = target.split("\\|");
        if (splits.length < 3) {
            throw new IllegalArgumentException("uri resource format error,please confirm config correctly.");
        }
        String      resource = splits[0].trim();
        Set<String> methods  = parseInfo(splits[1].trim().toLowerCase());
        Set<String> domains  = parseInfo(splits[2].trim());
        if (splits.length == 3) {
            return new UriResourceAuthority(resource,
                                            methods,
                                            domains,
                                            GrantMode.AND,
                                            Collections.emptySet(),
                                            Collections.emptySet());
        }
        Triple<GrantMode, Set<String>, Set<String>> triple = parseRoleOrAuthority(splits[3].trim());
        return new UriResourceAuthority(resource,
                                        methods,
                                        domains,
                                        triple.getLeft(),
                                        triple.getMiddle(),
                                        triple.getRight());
    }

    private static Set<String> parseInfo(String target) {
        if (StringUtils.isBlank(target) || target.contains(Permission.DEFAULT_VALUE)) {
            return Collections.emptySet();
        }
        return Arrays.stream(target.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    /**
     * 可能存在以下情况:
     * 1.*
     * 2.role(...) 或 permit(...)其中之一
     * 3.role(...) and(or) permit(...)
     */
    private static Triple<GrantMode, Set<String>, Set<String>> parseRoleOrAuthority(String target) {
        if (target.isBlank() || target.contains(Permission.DEFAULT_VALUE) || !target.endsWith(SUFFIX)) {
            return Triple.of(GrantMode.AND, Collections.emptySet(), Collections.emptySet());
        }
        if (!(target.contains(GrantMode.AND_LOWER)
                || target.contains(GrantMode.AND_UPPER)
                || target.contains(GrantMode.OR_LOWER)
                || target.contains(GrantMode.OR_UPPER))) {
            if (target.startsWith(PERMIT_PREFIX) && target.endsWith(SUFFIX)) {
                Set<String> authority = parseSplit(target, PERMIT_PREFIX);
                return Triple.of(GrantMode.AND, authority, Sets.newHashSet());
            }
            if (target.startsWith(ROLE_PREFIX) && target.endsWith(SUFFIX)) {
                Set<String> role = parseSplit(target, ROLE_PREFIX);
                return Triple.of(GrantMode.AND, Sets.newHashSet(), role);
            }
            throw new IllegalArgumentException("[" + target + "] role or permit config illegal.");
        }
        Pair<GrantMode, String> negotiated = negotiateMode(target);
        Assert.notNull(negotiated, "security mode must not null.");
        String[] split = target.trim().split(negotiated.getValue());
        Assert.state(split.length == 2, "[" + target + "] role or permit config illegal.");
        String first = split[0].trim();
        if (first.startsWith(ROLE_PREFIX) && first.endsWith(SUFFIX)) {
            return parse(split, negotiated.getKey(), false);
        }
        if (first.startsWith(PERMIT_PREFIX) && first.endsWith(SUFFIX)) {
            return parse(split, negotiated.getKey(), true);
        }
        throw new IllegalArgumentException("[" + target + "] role or permit config illegal.");
    }

    private static Pair<GrantMode, String> negotiateMode(String target) {
        if (target.contains(GrantMode.AND_LOWER)) {
            return Pair.of(GrantMode.AND, GrantMode.AND_LOWER);
        }
        if (target.contains(GrantMode.AND_UPPER)) {
            return Pair.of(GrantMode.AND, GrantMode.AND_UPPER);
        }
        if (target.contains(GrantMode.OR_LOWER)) {
            return Pair.of(GrantMode.OR, GrantMode.OR_LOWER);
        }
        if (target.contains(GrantMode.OR_UPPER)) {
            return Pair.of(GrantMode.OR, GrantMode.OR_UPPER);
        }
        return null;
    }

    private static Triple<GrantMode, Set<String>, Set<String>> parse(String[] values,
                                                                     GrantMode mode,
                                                                     boolean authFirst) {
        Set<String> authority = Sets.newHashSet();
        Set<String> role      = Sets.newHashSet();
        String      first     = values[0].trim();
        String      second    = values[1].trim();
        if (authFirst) {
            authority = parseSplit(first, PERMIT_PREFIX);
        } else if (second.startsWith(ROLE_PREFIX)) {
            role = parseSplit(second, ROLE_PREFIX);
        }
        if (!authFirst) {
            role = parseSplit(first, ROLE_PREFIX);
        } else if (second.startsWith(PERMIT_PREFIX)) {
            authority = parseSplit(second, PERMIT_PREFIX);
        }
        return Triple.of(mode, authority, role);
    }

    private static Set<String> parseSplit(String target, String prefix) {
        String replace = target.trim().replace(prefix, "").replace(SUFFIX, "");
        return Arrays.stream(replace.split(",")).map(String::trim).collect(Collectors.toSet());
    }

}
