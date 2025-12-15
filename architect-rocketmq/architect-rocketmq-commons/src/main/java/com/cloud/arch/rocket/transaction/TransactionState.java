package com.cloud.arch.rocket.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TransactionState {
    COMMIT(1),
    ROLLBACK(2),
    UNKNOWN(0);

    private final Integer state;

    public static Optional<TransactionState> valueOf(Integer state) {
        return Arrays.stream(values()).filter(v -> v.state.equals(state)).findFirst();
    }

}
