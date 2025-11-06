package com.cloud.arch.rocket.transaction;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionState {
    COMMIT(1),
    ROLLBACK(2),
    UNKNOWN(0);

    private final Integer state;

    TransactionState(Integer state) {
        this.state = state;
    }

    public static Optional<TransactionState> valueOf(Integer state) {
        return Arrays.stream(values())
                .filter(v -> v.state.equals(state))
                .findFirst();
    }

    public Integer getState() {
        return state;
    }
}
