package com.cloud.arch.mutex;

public class NotFoundMutexException extends RuntimeException {

    private static final long serialVersionUID = 7377336861850943L;

    public NotFoundMutexException() {
        super();
    }

    public NotFoundMutexException(String message) {
        super(message);
    }

    public NotFoundMutexException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundMutexException(Throwable cause) {
        super(cause);
    }

    protected NotFoundMutexException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
