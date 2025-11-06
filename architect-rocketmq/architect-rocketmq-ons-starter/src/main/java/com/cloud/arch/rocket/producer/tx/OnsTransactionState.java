package com.cloud.arch.rocket.producer.tx;

import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.cloud.arch.rocket.transaction.TransactionState;

import java.util.Arrays;

public enum OnsTransactionState {
    COMMIT(TransactionState.COMMIT, TransactionStatus.CommitTransaction),
    ROLLBACK(TransactionState.ROLLBACK, TransactionStatus.RollbackTransaction),
    UNKNOWN(TransactionState.UNKNOWN, TransactionStatus.Unknow);

    private final TransactionState  state;
    private final TransactionStatus status;

    OnsTransactionState(TransactionState state, TransactionStatus status) {
        this.state  = state;
        this.status = status;
    }

    public TransactionState getState() {
        return state;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * 获取ONS事务状态
     */
    public static TransactionStatus of(TransactionState state) {
        return Arrays.stream(values()).filter(e -> e.getState() == state).findFirst()
                     .map(OnsTransactionState::getStatus).orElse(TransactionStatus.Unknow);
    }
}
