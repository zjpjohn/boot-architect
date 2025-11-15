package com.cloud.arch.rocket.producer.tx;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.cloud.arch.rocket.transaction.TransactionCheckerContainer;
import com.cloud.arch.rocket.transaction.TransactionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalTransactionCheckerImpl implements LocalTransactionChecker {


    private final TransactionCheckerContainer checkerContainer;

    public LocalTransactionCheckerImpl(TransactionCheckerContainer checkerContainer) {
        this.checkerContainer = checkerContainer;
    }

    @Override
    public TransactionStatus check(Message message) {
        TransactionStatus transactionStatus = TransactionStatus.Unknow;
        try {
            TransactionState state = checkerContainer.checkTransaction(message.getTopic(),
                                                                       message.getTag(),
                                                                       message.getKey());
            transactionStatus = OnsTransactionState.of(state);
        } catch (Exception error) {
            log.error("check transaction message exception,the message key:{}", message.getKey(), error);
        }
        return transactionStatus;
    }

}
