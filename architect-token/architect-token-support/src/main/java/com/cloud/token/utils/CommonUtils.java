package com.cloud.token.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class CommonUtils {

    /**
     * 过期时间获取到期截止时间
     *
     * @param timeout 过期时间，时间单位-秒
     */
    public static long expireAt(long timeout, TimeUnit timeUnit) {
        if (timeout <= TokenConstants.NEVER_EXPIRE) {
            return TokenConstants.NEVER_EXPIRE;
        }
        return System.currentTimeMillis() + timeUnit.toMillis(timeout);
    }

    /**
     * key加前缀prefix
     *
     * @param prefix 前缀标识
     * @param key    key值
     */
    public static String key(String prefix, Object key) {
        return prefix + ":" + key;
    }

    /**
     * sessionId构建
     *
     * @param prefix 前缀
     * @param realm  业务域
     * @param key    登录标识
     */
    public static String sessionId(String prefix, String realm, Object key) {
        if (StringUtils.isBlank(realm)) {
            return prefix + ":" + key;
        }
        return prefix + ":" + realm + ":" + key;
    }

}
