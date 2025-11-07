package com.cloud.arch.web.support;

import com.cloud.arch.web.error.ErrorHandler;
import org.springframework.http.HttpStatus;

public enum AuthorizationErrorHandler implements ErrorHandler {
    HANDLE_ERROR(HttpStatus.NOT_FOUND, "your request not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error"),
    CHANNEL_NULL(HttpStatus.FORBIDDEN, "request domain null，please request domain"),
    CHANNEL_ERROR(HttpStatus.FORBIDDEN, "request domain error"),
    CHANNEL_FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden request domain"),
    ROLE_NULL(HttpStatus.FORBIDDEN, "no access privilege"),
    ROLE_FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden request"),
    AUTHORITY_FORBIDDEN(HttpStatus.FORBIDDEN, "no access privilege"),
    AUTHORITY_PROCESSOR_NONE(HttpStatus.INTERNAL_SERVER_ERROR, "none authority processor"),
    AUTH_IDENTITY_NONE(HttpStatus.UNAUTHORIZED, "user identity null."),
    AUTH_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "authority process internal error.");

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
