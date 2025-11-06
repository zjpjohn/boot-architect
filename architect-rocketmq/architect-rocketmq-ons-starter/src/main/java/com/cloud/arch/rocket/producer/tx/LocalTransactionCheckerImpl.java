package com.cloud.arch.rocket.producer.tx;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.cloud.arch.rocket.utils.HashUtil;
import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.transaction.TransactionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalTransactionCheckerImpl implements LocalTransactionChecker {

    private final TransactionChecker transactionChecker;

    public LocalTransactionCheckerImpl(TransactionChecker transactionChecker) {
        this.transactionChecker = transactionChecker;
    }

    @Override
    public TransactionStatus check(Message message) {
        long              crc32Id           = HashUtil.crc32code(message.getBody());
        TransactionStatus transactionStatus = TransactionStatus.Unknow;
        try {
            TransactionState state = transactionChecker.checkTransaction(message.getMsgID() + crc32Id);
            transactionStatus = OnsTransactionState.of(state);
        } catch (Exception e) {
            log.warn("check transaction message exception,the message id:{}", message.getMsgID());
        }
        return transactionStatus;
    }

}
