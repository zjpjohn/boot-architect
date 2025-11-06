package com.cloud.arch.web.custom;

import com.cloud.arch.web.props.WebmvcProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@UtilityClass
public class VersionConditionNegotiate {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.)+\\d");

    /**
     * 判断接口版本是否有效
     */
    public static boolean isValid(String version) {
        if (StringUtils.hasText(version)) {
            return VERSION_PATTERN.matcher(version).find();
        }
        return false;
    }

    /**
     * 获取请求版本号
     */
    public static RequestVersion getVersion(HttpServletRequest request, WebmvcProperties.VersionConfig config) {
        String name = config.getName();
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String version = config.isHeader() ? request.getHeader(name) : request.getParameter(name);
        if (isValid(version)) {
            return new RequestVersion(version);
        }
        return null;
    }

    /**
     * 判断请求版本号与当前版本号是否匹配
     */
    public static VersionRequestCondition negotiate(RequestVersion version, VersionRequestCondition condition) {
        if (version.compareTo(condition.version()) >= 0) {
            return condition;
        }
        return null;
    }

}
