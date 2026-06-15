package com.cloud.arch.idempotent;

import com.cloud.arch.mutex.MutexTemplate;
import com.cloud.arch.mutex.core.ContendMutexProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Slf4j
public class IdemCleanupScheduler implements SmartInitializingSingleton {

    private static final String CLEANUP_SQL      = "DELETE FROM arch_idempotent WHERE gmt_create < DATE_SUB(now(), INTERVAL :ttl SECOND)";
    private static final  String CLEAN_IDEM_MUTEX = "clean-idem-mutex";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MutexTemplate              mutexTemplate;
    private final IdempotentProperties       properties;

    public IdemCleanupScheduler(DataSource dataSource, MutexTemplate mutexTemplate, IdempotentProperties properties) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.mutexTemplate = mutexTemplate;
        this.properties = properties;
    }

    private void cleanup() {
        try {
            long                  ttl     = properties.getExpire().toSeconds();
            MapSqlParameterSource params  = new MapSqlParameterSource().addValue("ttl", ttl);
            int                   deleted = jdbcTemplate.update(CLEANUP_SQL, params);
            if (deleted > 0 && log.isDebugEnabled()) {
                log.debug("cleaned {} expired idempotent records", deleted);
            }
        } catch (Exception e) {
            log.warn("idempotent cleanup task error", e);
        }
    }

    private void cleanupSchedule() {
        final IdempotentProperties.SchedulerMutex mutex      = this.properties.getMutex();
        final ContendMutexProps                   mutexProps = new ContendMutexProps(mutex.getInitialDelay(), mutex.getTtl(), mutex.getTransition());
        mutexTemplate.scheduleAtRate(mutexProps, CLEAN_IDEM_MUTEX, properties.getInitialDelay(), properties.getInterval(), this::cleanup);
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.cleanupSchedule();
    }

}
