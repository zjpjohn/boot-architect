package com.cloud.arch.web.support;

import com.cloud.arch.web.error.ErrorHandler;
import org.springframework.http.HttpStatus;

public enum AuthorizationErrorHandler implements ErrorHandler {
    HANDLE_ERROR(HttpStatus.NOT_FOUND, "Your request not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    CHANNEL_NULL(HttpStatus.FORBIDDEN, "Request domain null,please request domain"),
    CHANNEL_ERROR(HttpStatus.FORBIDDEN, "Request domain error"),
    CHANNEL_FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden request domain"),
    ROLE_NULL(HttpStatus.FORBIDDEN, "No access privilege"),
    ROLE_FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden request"),
    AUTHORITY_FORBIDDEN(HttpStatus.FORBIDDEN, "No access privilege"),
    AUTHORITY_PROCESSOR_NONE(HttpStatus.INTERNAL_SERVER_ERROR, "No authority processor"),
    AUTH_IDENTITY_NONE(HttpStatus.UNAUTHORIZED, "Auth identity is null."),
    AUTH_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Authority process internal error.");

    private final HttpStatus status;
    private final Integer    code;
    private final String     error;

    AuthorizationErrorHandler(Integer code, String error, HttpStatus status) {
        this.status = status;
        this.code = code;
        this.error = error;
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
