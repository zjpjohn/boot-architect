package com.cloud.arch.idempotent.support;

import java.time.Duration;

public record IdempotentInfo(String key, String sharding, Duration duration, String message, boolean removeNow) {

    public IdempotentInfo(String key, Duration duration, String message) {
        this(key, "", duration, message, false);
    }

}
