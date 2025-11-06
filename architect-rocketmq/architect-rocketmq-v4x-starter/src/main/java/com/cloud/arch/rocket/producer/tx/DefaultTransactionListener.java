package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.transaction.TransactionState;
import com.cloud.arch.rocket.utils.CheckedConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

@Slf4j
public class DefaultTransactionListener implements TransactionListener {

    private final TransactionChecker transactionChecker;

    public DefaultTransactionListener(TransactionChecker transactionChecker) {
        this.transactionChecker = transactionChecker;
    }

    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
        RocketTransactionState transactionState = RocketTransactionState.UNKNOWN;
        String                 transactionId    = message.getTransactionId();
        try {
            //本地业务事务开启
            transactionChecker.begin();
            try {
                //业务处理操作
                transactionChecker.handle(null, CheckedConsumer.apply(args -> TransactionExecutorContext.execute()));
                transactionState = RocketTransactionState.COMMIT;
            } catch (Exception e) {
                log.error("handle local business transactionId:{},throw error:", transactionId, e);
                //本地业务执行失败
                transactionState = RocketTransactionState.ROLLBACK;
            }
            //本地事务状态存储操作，采用PROPAGATION_REQUIRES_NEW事务传播行为与本地业务执行保持一致
            //存在以下情况:
            // 1.业务执行成功，本地事务表执行成功;
            // 2.本地业务执行成功，本地事务表执行失败(本地事务表提交失败影响外层业务事务);
            // 3.本地业务执行失败，本地事务表执行成功(本地业务外层事务回滚不影响本地事务表的提交成功);
            // 4.本地业务执行失败，本事事务表执行失败
            transactionChecker.mark(transactionId, transactionState.getState());
            //业务处理事务提交
            transactionChecker.commit();
        } catch (Exception e) {
            log.error("jdbc transaction state transactionId:{},throw error:", transactionId, e);
            transactionState = RocketTransactionState.ROLLBACK;
            //本地业务事务操作回滚
            transactionChecker.rollback();
        }
        return transactionState.getTransactionState();
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        TransactionState state = transactionChecker.checkTransaction(msg.getTransactionId());
        return RocketTransactionState.of(state);
    }
}
