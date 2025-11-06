package com.cloud.token.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class FuzzyMatcher {

    private final String  pattern;
    private       Pattern matcher;

    public FuzzyMatcher(String pattern) {
        this.pattern = pattern;
        if (pattern.contains("*")) {
            matcher = Pattern.compile(pattern.replaceAll("\\*", ".*"));
        }
    }

    public boolean match(String target) {
        if (StringUtils.isBlank(target)) {
            return false;
        }
        if (matcher != null) {
            return matcher.matcher(target).find();
        }
        return pattern.contains(target);
    }

}
