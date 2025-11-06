package com.cloud.arch.operate.infrast.error;

import com.cloud.arch.web.error.ErrorHandler;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum OperationHandler implements ErrorHandler {
    LOG_NOT_EXIST(HttpStatus.BAD_REQUEST, 404, "操作日志不存在"),
    ;
    private final HttpStatus status;
    private final Integer    code;
    private final String     error;


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
