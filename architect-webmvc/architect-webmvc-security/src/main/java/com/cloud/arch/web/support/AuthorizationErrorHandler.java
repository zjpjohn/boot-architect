package com.cloud.arch.web.support;

import com.cloud.arch.web.error.ErrorHandler;
import org.springframework.http.HttpStatus;

public enum AuthorizationErrorHandler implements ErrorHandler {
    HANDLE_ERROR(HttpStatus.NOT_FOUND, "请求地址不存在"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误"),
    CHANNEL_NULL(HttpStatus.FORBIDDEN, "请求渠道为空，请配置渠道！"),
    CHANNEL_ERROR(HttpStatus.FORBIDDEN, "请求渠道错误"),
    CHANNEL_FORBIDDEN(HttpStatus.FORBIDDEN, "请求渠道禁止访问"),
    ROLE_NULL(HttpStatus.FORBIDDEN, "用户无权访问该接口"),
    ROLE_FORBIDDEN(HttpStatus.FORBIDDEN, "用户禁止访问该接口"),
    AUTHORITY_FORBIDDEN(HttpStatus.FORBIDDEN, "用户无权访问该接口"),
    AUTHORITY_PROCESSOR_NONE(HttpStatus.INTERNAL_SERVER_ERROR, "鉴权处理器为空"),
    AUTH_IDENTITY_NONE(HttpStatus.UNAUTHORIZED, "用户标识为空."),
    AUTH_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "权限校验内部错误.");

    private final HttpStatus status;
    private final Integer    code;
    private final String     error;

    AuthorizationErrorHandler(Integer code, String error, HttpStatus status) {
        this.status = status;
        this.code   = code;
        this.error  = error;
    }

    AuthorizationErrorHandler(HttpStatus status, String error) {
        this(status.value(), error, status);
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
