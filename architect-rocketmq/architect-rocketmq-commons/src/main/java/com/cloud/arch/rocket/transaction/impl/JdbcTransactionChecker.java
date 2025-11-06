package com.cloud.arch.rocket.transaction.impl;

import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.transaction.TransactionState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

@Getter
@Slf4j
public class JdbcTransactionChecker implements TransactionChecker {

    private static final String INSERT_SQL  = "insert into mq_transaction(tx_id,state,gmt_create) values(?,?,?)";
    private static final String QUERY_SQL   = "select state from mq_transaction where tx_id=?";
    private static final String GARBAGE_SQL = "delete from mq_transaction where gmt_create<?";

    private static final ThreadLocal<TransactionStatus> currentStatus = new ThreadLocal<>();

    private final JdbcTemplate                 jdbcTemplate;
    private final DataSourceTransactionManager transactionManager;

    public JdbcTransactionChecker(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.jdbcTemplate       = new JdbcTemplate(Objects.requireNonNull(transactionManager.getDataSource()));
    }

    /**
     * 开始本地事务
     */
    @Override
    public void begin() {
        currentStatus.set(this.transactionManager.getTransaction(new DefaultTransactionAttribute()));
    }

    /**
     * 提交本地事务
     */
    @Override
    public void commit() {
        try {
            transactionManager.commit(currentStatus.get());
        } finally {
            currentStatus.remove();
        }
    }

    /**
     * 回滚本地事务
     */
    @Override
    public void rollback() {
        try {
            transactionManager.rollback(currentStatus.get());
        } finally {
            currentStatus.remove();
        }
    }

    /**
     * 标记本地事务
     *
     * @param key   事务标识
     * @param state 事务状态
     */
    @Override
    public void mark(String key, TransactionState state) {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(attribute);
        try {
            jdbcTemplate.update(INSERT_SQL, key, state.getState(), new Date());
            transactionManager.commit(status);
        } catch (TransactionException e) {
            transactionManager.rollback(status);
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查本地事务
     *
     * @param key 事务标识
     */
    @Override
    public TransactionState checkTransaction(String key) {
        Integer state = 0;
        try {
            state = jdbcTemplate.queryForObject(QUERY_SQL, Integer.class, key);
        } catch (DataAccessException e) {
            log.warn("query rocket transaction status error.", e);
        }
        return TransactionState.valueOf(state).orElse(TransactionState.UNKNOWN);
    }

    /**
     * 回收本地事务状态信息
     * 清理历史数据
     */
    @Override
    public void garbageState(Date date) {
        jdbcTemplate.update(GARBAGE_SQL, date);
    }

    /**
     * 执行本地事务业务
     *
     * @param args     业务参数
     * @param consumer 外部回调处理
     */
    @Override
    public void handle(Object[] args, Consumer<Object[]> consumer) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionAttribute());
        try {
            consumer.accept(args);
            transactionManager.commit(status);
        } catch (TransactionException e) {
            transactionManager.rollback(status);
            throw new RuntimeException(e);
        }
    }
}
