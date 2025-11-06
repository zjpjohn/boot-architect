package com.cloud.arch.rocket.idempotent;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Data
public class IdempotentCleanHandler {

    public static final Integer DEFAULT_INTERVAL = 7;

    private final IdempotentChecker idempotentChecker;

    public IdempotentCleanHandler(IdempotentChecker idempotentChecker) {
        this.idempotentChecker = idempotentChecker;
    }

    /**
     * 历史记录收集
     */
    public void handle(int interval) {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(interval <= 0 ? DEFAULT_INTERVAL : interval);
        this.idempotentChecker.garbageCollect(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

}
