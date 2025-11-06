package com.cloud.token.security;

/**
 * 权限校验模式
 */
public enum SecureMode {
    /**
     * 所有条件都满足
     */
    AND,
    /**
     * 至少其中之一满足
     */
    OR
}
