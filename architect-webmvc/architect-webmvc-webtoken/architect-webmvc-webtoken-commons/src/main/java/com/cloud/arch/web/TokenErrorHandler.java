package com.cloud.arch.web;

import com.cloud.arch.web.error.ErrorHandler;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum TokenErrorHandler implements ErrorHandler {
    AUTH_SOURCE_NONNULL(HttpStatus.BAD_REQUEST, "登录渠道为空."),
    AUTH_SOURCE_ERROR(HttpStatus.UNAUTHORIZED, "登录渠道错误."),
    INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "授权登录token无效."),
    AUTH_SOURCE_UNKNOWN(HttpStatus.UNAUTHORIZED, "登录渠道未知."),
    AUTH_IDENTITY_UNKNOWN(HttpStatus.UNAUTHORIZED, "用户登录标识为空.");

    private final HttpStatus status;
    private final Integer    code;
    private final String     error;

    TokenErrorHandler(HttpStatus status, String error) {
        this(status, status.value(), error);
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getError() {
        return this.error;
    }

}
