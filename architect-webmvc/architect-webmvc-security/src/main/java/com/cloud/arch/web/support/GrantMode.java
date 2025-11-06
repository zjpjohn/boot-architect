package com.cloud.arch.web.support;

public enum GrantMode {
    AND,
    OR;

    public static final String OR_LOWER  = "or";
    public static final String OR_UPPER  = "OR";
    public static final String AND_LOWER = "and";
    public static final String AND_UPPER = "AND";

    public static GrantMode of(String value) {
        if (OR_LOWER.equalsIgnoreCase(value)) {
            return OR;
        }
        if (AND_LOWER.equalsIgnoreCase(value)) {
            return AND;
        }
        throw new IllegalArgumentException(String.format("grant mode[%s] error,please confirm config.", value));
    }
}
