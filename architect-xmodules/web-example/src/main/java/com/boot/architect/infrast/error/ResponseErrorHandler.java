package com.boot.architect.infrast.error;

import com.cloud.arch.web.error.ErrorHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseErrorHandler implements ErrorHandler {
    AUTH_LOGIN_ERROR(HttpStatus.BAD_REQUEST, "授权登录错误");

    private final HttpStatus status;
    private final Integer    code;
    private final String     error;

    ResponseErrorHandler(HttpStatus status, String error) {
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
