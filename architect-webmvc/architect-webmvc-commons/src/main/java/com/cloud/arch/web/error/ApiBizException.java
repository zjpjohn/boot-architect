package com.cloud.arch.web.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Getter
public class ApiBizException extends RuntimeException {

    //业务错误响应状态
    private final HttpStatus   status;
    //业务错误标识
    private final Integer      code;
    //业务错误消息
    private final String       error;
    //错误响应数据
    private final Serializable data;

    public ApiBizException(Integer code, String error) {
        this(code, error, false);
    }

    public ApiBizException(Integer code, String error, boolean stacked) {
        this(HttpStatus.OK, code, error, stacked, null);
    }

    public ApiBizException(HttpStatus status, Integer code, String error) {
        this(status, code, error, false, null);
    }

    public ApiBizException(HttpStatus status, Integer code, String error, Serializable data) {
        this(status, code, error, false, data);
    }

    public ApiBizException(HttpStatus status, Integer code, String error, boolean writableStackTrace,
        Serializable data) {
        super(error, null, false, writableStackTrace);
        this.status = status;
        this.code   = code;
        this.error  = error;
        this.data   = data;
    }

    public static ApiBizException from(ErrorHandler handler) {
        return new ApiBizException(handler.getStatus(), handler.getCode(), handler.getError());
    }

    public static ApiBizException from(ErrorHandler handler, Serializable data) {
        return new ApiBizException(handler.getStatus(), handler.getCode(), handler.getError(), data);
    }

}
