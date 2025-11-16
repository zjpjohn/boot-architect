package com.cloud.arch.web.support;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.props.WebAuthorityProperties;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UriAuthorityManager implements DisposableBean {

    private final WebAuthorityProperties                         properties;
    //已配置的uri权限资源集合
    private final List<UriResourceAuthority>                     resources;
    //缓存标记未匹配权限的方法集合
    private final Set<AnnotatedElementKey>                       excludes         = Sets.newConcurrentHashSet();
    //缓存已匹配的权限资源方法
    private final Map<AnnotatedElementKey, UriResourceAuthority> cacheAuthorities = Maps.newConcurrentMap();
    //字符串分割器
    private final Splitter                                       splitter         = Splitter.on(",")
                                                                                            .trimResults()
                                                                                            .omitEmptyStrings();

    public UriAuthorityManager(WebAuthorityProperties properties) {
        this.properties = properties;
        this.resources  = properties.parse();
    }

    /**
     * 获取拦截器拦截的请求uri集合
     */
    public List<String> getPatternList() {
        List<String> authorities = this.resources.stream().map(UriResourceAuthority::getResource).toList();
        List<String> patternList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(authorities)) {
            return patternList;
        }
        String patterns = properties.getPatterns();
        if (patterns.equals(WebAuthorityProperties.DEFAULT_PATTERN)) {
            patternList.addAll(authorities);
            return patternList;
        }
        List<String> splits = splitter.splitToList(patterns);
        patternList.addAll(splits);
        return patternList;
    }

    /**
     * 获取拦截器排除的请求uri集合
     */
    public List<String> getExcudeList() {
        Set<String> values     = Sets.newHashSet(WebTokenConstants.STATIC_RESOURCES);
        String      excludeStr = properties.getExcludes();
        if (StringUtils.isNotBlank(excludeStr)) {
            List<String> appended = splitter.splitToList(excludeStr);
            values.addAll(appended);
        }
        return Lists.newArrayList(values);
    }

    /**
     * 获取请求对应的权限资源
     */
    public UriResourceAuthority measureAuthority(HttpServletRequest request, HandlerMethod handlerMethod) {
        if (CollectionUtils.isEmpty(this.resources)) {
            return null;
        }
        Class<?>            beanType   = handlerMethod.getBeanType();
        AnnotatedElementKey elementKey = new AnnotatedElementKey(handlerMethod.getMethod(), beanType);
        if (excludes.contains(elementKey)) {
            return null;
        }
        UriResourceAuthority authority = cacheAuthorities.get(elementKey);
        if (authority != null) {
            return authority;
        }
        String requestURI = request.getRequestURI();
        String method     = request.getMethod();
        //已匹配方法的权限校验资源进行缓存，未匹配的方法进行标记
        return resources.stream()
                        .filter(r -> r.match(requestURI, method))
                        .findFirst()
                        .map(v -> cacheAuthorities.put(elementKey, v))
                        .orElseGet(() -> {
                            excludes.add(elementKey);
                            return null;
                        });
    }

    @Override
    public void destroy() throws Exception {
        excludes.clear();
        cacheAuthorities.clear();
    }

}
