package com.cloud.arch.web;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static com.cloud.arch.web.TokenAuthProperties.PROPS_PREFIX;

@Data
@ConfigurationProperties(prefix = PROPS_PREFIX)
public class TokenAuthProperties {

    public static final String PROPS_PREFIX    = "com.cloud.web.auth";
    public static final String DEFAULT_PATTERN = "/*";

    /**
     * 是否开启token授权验证，仅在springboot环境下起作用
     */
    private boolean enable   = true;
    /**
     * 拦截请求url正则集合,多个正则用','分隔
     */
    private String  patterns = DEFAULT_PATTERN;
    /**
     * 不拦截请求url正则集合,多个用','分隔
     */
    private String  excludes;

    public List<String> patterns() {
        if (StringUtils.isBlank(this.patterns)) {
            return Lists.newArrayList(DEFAULT_PATTERN);
        }
        return Splitter.on(",").splitToList(this.patterns);
    }

    public List<String> excludes() {
        List<String> values = Lists.newArrayList(WebTokenConstants.STATIC_RESOURCES);
        if (StringUtils.isBlank(this.excludes)) {
            return values;
        }
        List<String> append = Splitter.on(",").trimResults().splitToList(this.excludes);
        values.addAll(append);
        return values;
    }

}
