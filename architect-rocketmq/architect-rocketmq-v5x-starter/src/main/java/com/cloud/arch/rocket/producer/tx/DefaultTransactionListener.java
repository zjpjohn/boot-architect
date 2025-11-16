package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.transaction.TransactionCheckerContainer;
import com.cloud.arch.rocket.transaction.TransactionState;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

@Slf4j
public class DefaultTransactionListener implements TransactionListener {

    private final TransactionCheckerContainer checkerContainer;

    public DefaultTransactionListener(TransactionCheckerContainer checkerContainer) {
        this.checkerContainer = checkerContainer;
    }

    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
        RocketTransactionState transactionState = RocketTransactionState.UNKNOWN;
        String                 transactionId    = message.getTransactionId();
        try {
            TransactionExecutorContext.execute();
            transactionState = RocketTransactionState.COMMIT;
        } catch (Throwable e) {
            log.error("handle local business transactionId:{},throw error:", transactionId, e);
            transactionState = RocketTransactionState.ROLLBACK;
        }
        return transactionState.getTransactionState();
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        LocalTransactionState transactionState = LocalTransactionState.UNKNOW;
        try {
            TransactionState state = checkerContainer.checkTransaction(msg.getTopic(), msg.getTags(), msg.getKeys());
            transactionState = RocketTransactionState.of(state);
        } catch (Exception error) {
            log.error("check transaction message exception,the message key:{}", msg.getKeys(), error);
        }
        return transactionState;
    }

}
