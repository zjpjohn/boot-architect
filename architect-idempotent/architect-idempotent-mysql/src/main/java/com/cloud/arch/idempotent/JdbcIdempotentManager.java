package com.cloud.arch.idempotent;

import com.cloud.arch.idempotent.support.IdempotentInfo;
import com.cloud.arch.idempotent.support.IdempotentManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@Slf4j
@Getter
public class JdbcIdempotentManager implements IdempotentManager {

    private static final String INSERT_SQL = "insert ignore into arch_idempotent(lock_key,sharding,gmt_create)"
            + " values(:lock_key,:sharding,current_time);";

    private static final ThreadLocal<TransactionStatus> localStatus = new ThreadLocal<>();

    private final NamedParameterJdbcTemplate   jdbcTemplate;
    private final DataSourceTransactionManager transactionManager;

    public JdbcIdempotentManager(DataSource dataSource, DataSourceTransactionManager transactionManager) {
        this.jdbcTemplate       = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean tryAcquire(IdempotentInfo idempotent) {
        localStatus.set(transactionManager.getTransaction(new DefaultTransactionAttribute()));
        MapSqlParameterSource paramSource = new MapSqlParameterSource().addValue("lock_key", idempotent.key())
                                                                       .addValue("sharding", idempotent.sharding());
        return jdbcTemplate.update(INSERT_SQL, paramSource) > 0;
    }

    @Override
    public void completed(IdempotentInfo idempotent, Throwable throwable) {
        try {
            TransactionStatus status = localStatus.get();
            if (throwable != null) {
                transactionManager.rollback(status);
                return;
            }
            transactionManager.commit(status);
        } finally {
            localStatus.remove();
        }
    }

}
