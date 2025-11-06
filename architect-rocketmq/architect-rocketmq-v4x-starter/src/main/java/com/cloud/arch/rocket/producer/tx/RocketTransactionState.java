package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.transaction.TransactionState;
import org.apache.rocketmq.client.producer.LocalTransactionState;

import java.util.Arrays;

public enum RocketTransactionState {

    COMMIT(TransactionState.COMMIT, LocalTransactionState.COMMIT_MESSAGE),
    ROLLBACK(TransactionState.ROLLBACK, LocalTransactionState.ROLLBACK_MESSAGE),
    UNKNOWN(TransactionState.UNKNOWN, LocalTransactionState.UNKNOW);

    private final TransactionState      state;
    private final LocalTransactionState transactionState;

    RocketTransactionState(TransactionState state, LocalTransactionState transactionState) {
        this.state            = state;
        this.transactionState = transactionState;
    }

    public TransactionState getState() {
        return state;
    }

    public LocalTransactionState getTransactionState() {
        return transactionState;
    }

    public static LocalTransactionState of(TransactionState state) {
        return Arrays.stream(values()).filter(e -> e.state == state).findFirst()
                     .map(RocketTransactionState::getTransactionState).orElse(LocalTransactionState.UNKNOW);
    }

}
