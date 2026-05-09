package com.cloud.arch.event.subscribe.impl;

import com.cloud.arch.event.subscribe.AbstractIdempotentChecker;
import com.cloud.arch.event.subscribe.EventIdempotent;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * 基于 JDBC 的幂等检查器，通过 INSERT IGNORE 去重、DELETE 清理，实现消息消费的幂等保证。
 */
@Getter
public class JdbcIdempotentChecker extends AbstractIdempotentChecker {

    private static final String INSERT_SQL     = "insert ignore into arch_event_idempot(`name`,filter,event_key,shard_key,gmt_create) values(?,?,?,?,?)";
    private static final String DELETE_KEY_SQL = "delete from arch_event_idempot where `name`=? and filter=? and event_key=? and shard_key=?";
    private static final String GARBAGE_SQL    = "delete from arch_event_idempot where gmt_create<?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcIdempotentChecker(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean doProcessed(EventIdempotent idempotent) throws Exception {
        return jdbcTemplate.update(INSERT_SQL,
                                   idempotent.getName(),
                                   idempotent.getFilter(),
                                   idempotent.getEventKey(),
                                   idempotent.getShardKey(),
                                   LocalDateTime.now()) != 1;
    }

    /**
     * 标记消息处理完成，成功后无需额外操作（INSERT IGNORE 已记录幂等）。
     */
    @Override
    public void markSuccess(EventIdempotent idempotent) {

    }

    /**
     * 标记消息处理失败
     *
     * @param idempotent 幂等信息
     */
    @Override
    public void markFailed(EventIdempotent idempotent) {
        jdbcTemplate.update(DELETE_KEY_SQL,
                            idempotent.getName(),
                            idempotent.getFilter(),
                            idempotent.getEventKey(),
                            idempotent.getShardKey());
    }

    /**
     * 回收幂等记录
     *
     * @param before 回收时间
     */
    @Override
    public void garbageClean(LocalDateTime before) {
        jdbcTemplate.update(GARBAGE_SQL, before);
    }

}
