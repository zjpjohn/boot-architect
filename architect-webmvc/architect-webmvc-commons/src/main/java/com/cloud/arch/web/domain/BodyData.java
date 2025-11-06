package com.cloud.arch.web.domain;

public record BodyData<R>(String error, String message, Integer code, R data, Long timestamp) {

    public BodyData(String message, Integer code, R data) {
        this(null, message, code, data);
    }

    public BodyData(String error, String message, Integer code) {
        this(error, message, code, null);
    }

    public BodyData(String error, Integer code) {
        this(error, null, code, null);
    }

    public BodyData(String error, String message, Integer code, R data) {
        this(error, message, code, data, System.currentTimeMillis());
    }

}
