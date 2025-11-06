package com.cloud.arch.rocket.idempotent.impl;


import com.cloud.arch.rocket.idempotent.AbstractIdempotentCheck;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Date;

public class JdbcIdempotentChecker extends AbstractIdempotentCheck {

    private static final String INSERT_SQL     = "insert ignore into mq_idempotent(k,cls,gmt_create) values(?,?,?)";
    private static final String DELETE_KEY_SQL = "delete from mq_idempotent where k=? and cls=?";
    private static final String GARBAGE_SQL    = "delete from mq_idempotent where gmt_create<?";

    private JdbcTemplate jdbcTemplate;

    public JdbcIdempotentChecker(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public boolean doProcessed(String key, Integer cls) throws Exception {
        return jdbcTemplate.update(INSERT_SQL, key, cls, new Date()) != 1;
    }

    /**
     * 标记消息处理完成
     *
     * @param key 消息标识
     */
    @Override
    public void markSuccess(String key, Integer cls) {

    }

    /**
     * 标记消息处理失败
     *
     * @param key 消息标识
     */
    @Override
    public void markFailed(String key, Integer cls) {
        jdbcTemplate.update(DELETE_KEY_SQL, key, cls);
    }

    /**
     * 回收处理
     *
     * @param before 回收时间
     */
    @Override
    public void garbageCollect(Date before) {
        jdbcTemplate.update(GARBAGE_SQL, before);
    }
}
