package com.cloud.arch.rocket.idempotent.impl;

import com.cloud.arch.rocket.idempotent.AbstractIdempotentCheck;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.util.Date;

public class TransactionIdempotentChecker extends AbstractIdempotentCheck {

    private static final ThreadLocal<TransactionStatus> currentStatus = new ThreadLocal<>();

    private DataSourceTransactionManager transactionManager;
    private AbstractIdempotentCheck      idempotentCheck;

    public TransactionIdempotentChecker(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.idempotentCheck    = new JdbcIdempotentChecker(transactionManager.getDataSource());
    }

    public DataSourceTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public AbstractIdempotentCheck getIdempotentCheck() {
        return idempotentCheck;
    }

    public void setIdempotentCheck(AbstractIdempotentCheck idempotentCheck) {
        this.idempotentCheck = idempotentCheck;
    }

    @Override
    public boolean doProcessed(String key, Integer cls) throws Exception {
        currentStatus.set(this.transactionManager.getTransaction(new DefaultTransactionAttribute()));
        return this.idempotentCheck.doProcessed(key, cls);
    }

    /**
     * 标记消息处理完成
     *
     * @param key 消息标识
     */
    @Override
    public void markSuccess(String key, Integer cls) {
        try {
            TransactionStatus status = currentStatus.get();
            transactionManager.commit(status);
        } finally {
            currentStatus.remove();
        }
    }

    /**
     * 标记消息处理失败
     *
     * @param key 消息标识
     */
    @Override
    public void markFailed(String key, Integer cls) {
        try {
            TransactionStatus status = currentStatus.get();
            this.transactionManager.rollback(status);
        } finally {
            currentStatus.remove();
        }
    }

    /**
     * 回收处理
     *
     * @param before 回收时间
     */
    @Override
    public void garbageCollect(Date before) {
        this.idempotentCheck.garbageCollect(before);
    }
}
