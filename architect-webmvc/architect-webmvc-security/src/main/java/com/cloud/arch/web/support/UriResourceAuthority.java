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
     * 请求资源允许的访问权限集合，为空表示全部权限
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
            throw new IllegalArgumentException("at least one of domains, roles, authorities must not be null.");
        }
        this.mode = mode;
        this.resource = resource;
        this.methods = methods;
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
     * 判断目标访问域是否有效
     */
    public boolean isValidDomain(String target) {
        return StringUtils.isNotBlank(target) &&
               (CollectionUtils.isEmpty(this.domains) || this.domains.contains(target));
    }

    /**
     * 构建鉴权信息
     */
    public GrantAuthority authority(String identity) {
        return new GrantAuthority(identity, mode, this.roles, this.permits);
    }

    /**
     * 空权限或角色判断
     */
    public boolean isEmptyRoleAndPermits() {
        return CollectionUtils.isEmpty(roles) && CollectionUtils.isEmpty(permits);
    }

    /**
     * 资源匹配计算
     *
     * @param requestUri 请求uri
     * @param method     请求方法
     */
    public boolean match(String requestUri, String method) {
        return (CollectionUtils.isEmpty(this.methods) || this.methods.contains(method.toLowerCase())) &&
               antPathMatcher.match(this.resource, requestUri);
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

    /**
     * 解析逗号分隔的字符串为 Set，空值或通配符返回空集合
     */
    private static Set<String> parseInfo(String target) {
        if (StringUtils.isBlank(target) || target.contains(Permission.DEFAULT_VALUE)) {
            return Collections.emptySet();
        }
        return Arrays.stream(target.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    /**
     * 解析角色权限表达式，可能存在以下情况:
     * 1. * 通配符 → 返回空集合
     * 2. role(...) 或 permit(...) 单一表达式 → 委托 parseSingle
     * 3. role(...) and(or) permit(...) 组合表达式 → 委托 parseComposite
     */
    private static Triple<GrantMode, Set<String>, Set<String>> parseRoleOrAuthority(String target) {
        if (isWildcard(target)) {
            return Triple.of(GrantMode.AND, Collections.emptySet(), Collections.emptySet());
        }
        return isComposite(target) ? parseComposite(target) : parseSingle(target);
    }

    /**
     * 判断是否为通配符/空值，无需解析角色和权限
     */
    private static boolean isWildcard(String target) {
        return target.isBlank() || target.contains(Permission.DEFAULT_VALUE) || !target.endsWith(SUFFIX);
    }

    /**
     * 判断是否为组合表达式（包含 and 或 or 关键字）
     */
    private static boolean isComposite(String target) {
        return target.contains(GrantMode.AND_LOWER) ||
               target.contains(GrantMode.AND_UPPER) ||
               target.contains(GrantMode.OR_LOWER) ||
               target.contains(GrantMode.OR_UPPER);
    }

    /**
     * 解析单一角色或权限表达式，如 permit(xxx) 或 role(xxx)
     */
    private static Triple<GrantMode, Set<String>, Set<String>> parseSingle(String target) {
        if (target.startsWith(PERMIT_PREFIX) && target.endsWith(SUFFIX)) {
            return Triple.of(GrantMode.AND, parseSplit(target, PERMIT_PREFIX), Sets.newHashSet());
        }
        if (target.startsWith(ROLE_PREFIX) && target.endsWith(SUFFIX)) {
            return Triple.of(GrantMode.AND, Sets.newHashSet(), parseSplit(target, ROLE_PREFIX));
        }
        throw new IllegalArgumentException("[" + target + "] role or permit config illegal.");
    }

    /**
     * 解析组合角色权限表达式，如 role(xxx) and permit(yyy)
     * 先协商连接模式(and/or)，再按分隔符拆分为两段分别解析
     */
    private static Triple<GrantMode, Set<String>, Set<String>> parseComposite(String target) {
        Pair<GrantMode, String> mode = negotiateMode(target);
        Assert.notNull(mode, "security mode must not null.");
        String[] segments = target.trim().split(mode.getValue());
        Assert.state(segments.length == 2, "[" + target + "] role or permit config illegal.");
        String first = segments[0].trim();
        if (first.startsWith(ROLE_PREFIX) && first.endsWith(SUFFIX)) {
            return parseCompositeSegments(segments, mode.getKey(), false);
        }
        if (first.startsWith(PERMIT_PREFIX) && first.endsWith(SUFFIX)) {
            return parseCompositeSegments(segments, mode.getKey(), true);
        }
        throw new IllegalArgumentException("[" + target + "] role or permit config illegal.");
    }

    /**
     * 从表达式中协商连接模式（and/or），返回模式及其字符串表示
     */
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

    /**
     * 按分隔符拆分的两段表达式分别解析角色和权限，permitFirst 控制左右哪侧为权限
     */
    private static Triple<GrantMode, Set<String>, Set<String>> parseCompositeSegments(String[] segments,
                                                                                      GrantMode mode,
                                                                                      boolean permitFirst) {
        String      left    = segments[0].trim();
        String      right   = segments[1].trim();
        Set<String> permits = permitFirst ? parseSplit(left, PERMIT_PREFIX) : parseSplit(right, PERMIT_PREFIX);
        Set<String> roles   = permitFirst ? parseSplit(right, ROLE_PREFIX) : parseSplit(left, ROLE_PREFIX);
        return Triple.of(mode, permits, roles);
    }

    /**
     * 去除表达式的前缀和右括号，按逗号拆分提取值集合
     */
    private static Set<String> parseSplit(String target, String prefix) {
        String replace = target.trim().replace(prefix, "").replace(SUFFIX, "");
        return Arrays.stream(replace.split(",")).map(String::trim).collect(Collectors.toSet());
    }

}
