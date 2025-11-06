package com.cloud.arch.aggregate;

public class AggregateException extends RuntimeException{

    public AggregateException() {
        super();
    }

    public AggregateException(String message) {
        super(message);
    }

    public AggregateException(String message, Throwable cause) {
        super(message, cause);
    }

    public AggregateException(Throwable cause) {
        super(cause);
    }

    protected AggregateException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
