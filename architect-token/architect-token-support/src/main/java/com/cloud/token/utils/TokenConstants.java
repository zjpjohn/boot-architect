package com.cloud.token.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenConstants {

    /**
     * token key前缀标识
     */
    public static final String TOKEN_PREFIX   = "t";
    /**
     * session key前缀标识
     */
    public static final String SESSION_PREFIX = "s";

    /**
     * token无过期标识
     */
    public static final Long   NEVER_EXPIRE        = -1L;
    /**
     * 没有过期时间标识
     */
    public static final Long   NO_EXPIRE_VALUE     = -2L;
    /**
     * 默认登录设备标识
     */
    public static final String DEFAULT_DEVICE      = "default";
    /**
     * 默认类型领域
     */
    public static final String DEFAULT_REALM       = "default";
    /**
     * 封禁账号默认封禁等级
     */
    public static final int    DEFAULT_MUTED_LEVEL = 1;
    /**
     * 封禁账号最小封禁等级
     */
    public static final int    MIN_MUTED_LEVEL     = 1;
    /**
     * 封禁账号时，未封禁账号
     */
    public static final int    NO_MUTED_LEVEL      = -2;
    /**
     * token二次验证时写入的value值
     */
    public static final String AUTH_SAFE_VALUE     = "safe";
}
