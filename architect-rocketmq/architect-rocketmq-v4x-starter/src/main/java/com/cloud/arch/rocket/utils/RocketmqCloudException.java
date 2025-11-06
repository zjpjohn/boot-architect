package com.cloud.arch.rocket.utils;

public class RocketmqCloudException extends RuntimeException {

    public RocketmqCloudException() {
        super();
    }

    public RocketmqCloudException(String message) {
        super(message);
    }


    public RocketmqCloudException(String message, Throwable cause) {
        super(message, cause);
    }

    public RocketmqCloudException(Throwable cause) {
        super(cause);
    }
}
