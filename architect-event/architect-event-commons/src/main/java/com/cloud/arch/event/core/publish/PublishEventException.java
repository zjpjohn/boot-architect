package com.cloud.arch.event.core.publish;

public class PublishEventException extends RuntimeException {

    private static final long serialVersionUID = -201183861415745997L;

    public PublishEventException() {
    }

    public PublishEventException(String message) {
        super(message);
    }

    public PublishEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
