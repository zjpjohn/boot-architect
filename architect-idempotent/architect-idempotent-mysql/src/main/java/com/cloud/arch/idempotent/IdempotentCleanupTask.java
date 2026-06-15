package com.cloud.arch.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IdempotentCleanupTask implements DisposableBean {

    private static final String CLEANUP_SQL = "DELETE FROM arch_idempotent WHERE gmt_create < DATE_SUB(now(), INTERVAL :ttl SECOND)";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ScheduledExecutorService   scheduler;

    public IdempotentCleanupTask(DataSource dataSource, long intervalSeconds, long recordTtlSeconds) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        long ttl    = Math.max(10, recordTtlSeconds);
        long period = Math.max(10, intervalSeconds);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "idempotent-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleWithFixedDelay(() -> cleanup(ttl), period, period, TimeUnit.SECONDS);
    }

    private void cleanup(long ttl) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("ttl", ttl);
            int deleted = jdbcTemplate.update(CLEANUP_SQL, params);
            if (deleted > 0 && log.isDebugEnabled()) {
                log.debug("cleaned {} expired idempotent records", deleted);
            }
        } catch (Exception e) {
            log.warn("idempotent cleanup task error", e);
        }
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
    }
}
