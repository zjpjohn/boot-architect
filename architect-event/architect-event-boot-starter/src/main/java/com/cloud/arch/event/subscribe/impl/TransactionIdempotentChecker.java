package com.cloud.arch.event.subscribe.impl;

import com.cloud.arch.event.subscribe.AbstractIdempotentChecker;
import com.cloud.arch.event.subscribe.EventIdempotent;
import lombok.Getter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.time.LocalDateTime;

@Getter
public class TransactionIdempotentChecker extends AbstractIdempotentChecker {

    private static final ThreadLocal<TransactionStatus> localStatus = new ThreadLocal<>();

    private final DataSourceTransactionManager transactionManager;
    private final AbstractIdempotentChecker    idempotentChecker;

    public TransactionIdempotentChecker(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        idempotentChecker       = new JdbcIdempotentChecker(transactionManager.getDataSource());
    }

    @Override
    public boolean doProcessed(EventIdempotent idempotent) throws Exception {
        localStatus.set(this.transactionManager.getTransaction(new DefaultTransactionAttribute()));
        return this.idempotentChecker.doProcessed(idempotent);
    }

    /**
     * 标记消息处理完成
     *
     * @param idempotent 幂等信息
     */
    @Override
    public void markSuccess(EventIdempotent idempotent) {
        try {
            TransactionStatus status = localStatus.get();
            transactionManager.commit(status);
        } finally {
            localStatus.remove();
        }
    }

    /**
     * 标记消息处理失败
     *
     * @param idempotent 幂等信息
     */
    @Override
    public void markFailed(EventIdempotent idempotent) {
        try {
            TransactionStatus status = localStatus.get();
            this.transactionManager.rollback(status);
        } finally {
            localStatus.remove();
        }
    }

    /**
     * 回收幂等记录
     *
     * @param before 回收时间
     */
    @Override
    public void garbageClean(LocalDateTime before) {
        this.idempotentChecker.garbageClean(before);
    }
}
