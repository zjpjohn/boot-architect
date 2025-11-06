package com.cloud.arch.rocket.transaction;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Data
public class TransactionCleanHandler {

    public static final Integer DEFAULT_INTERVAL = 7;

    private final TransactionChecker transactionChecker;

    public TransactionCleanHandler(TransactionChecker transactionChecker) {
        this.transactionChecker = transactionChecker;
    }

    public void handle(int interval) {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(interval <= 0 ? DEFAULT_INTERVAL : interval);
        this.transactionChecker.garbageState(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
    }
}
