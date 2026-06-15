package com.cloud.arch.idempotent.extension;

import com.cloud.arch.idempotent.IdempotentCleanupTask;
import com.cloud.arch.idempotent.JdbcIdempotentManager;
import com.cloud.arch.idempotent.boot.IdempotentProperties;
import com.cloud.arch.idempotent.support.IdempotentManager;
import com.cloud.arch.mutex.MutexTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Duration;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(IdempotentProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.cloud.arch.idempotent.JdbcIdempotentManager")
public class JdbcIdempotentConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(DataSourceTransactionManager.class)
    public DataSourceTransactionManager txManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Primary
    @ConditionalOnBean(DataSource.class)
    public IdempotentManager idempotentManager(DataSource dataSource, DataSourceTransactionManager transactionManager) {
        return new JdbcIdempotentManager(dataSource, transactionManager);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingClass("com.cloud.arch.mutex.MutexTemplate")
    @ConditionalOnProperty(prefix = "com.cloud.idempotent.mysql.cleanup", name = "enabled", havingValue = "true")
    public IdempotentCleanupTask cleanupTask(DataSource dataSource, IdempotentProperties properties) {
        return new IdempotentCleanupTask(dataSource, properties.getCleanupInterval(), properties.getRecordTtl());
    }

    @Configuration
    @ConditionalOnClass(name = "com.cloud.arch.mutex.MutexTemplate")
    static class DistributedCleanupConfiguration {

        private static final String CLEANUP_SQL = "DELETE FROM arch_idempotent WHERE gmt_create < DATE_SUB(now(), INTERVAL :ttl SECOND)";

        @Bean
        @ConditionalOnBean({DataSource.class, MutexTemplate.class})
        @ConditionalOnProperty(prefix = "com.cloud.idempotent.mysql.cleanup", name = "enabled", havingValue = "true")
        public Object distributedCleanupRegistrar(MutexTemplate mutexTemplate, DataSource dataSource, IdempotentProperties properties) {
            NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(dataSource);
            long ttl    = Math.max(10, properties.getRecordTtl());
            long period = Math.max(10, properties.getCleanupInterval());
            mutexTemplate.scheduleAtRate(
                "idempotent-cleanup",
                Duration.ofSeconds(period),
                Duration.ofSeconds(period),
                () -> {
                    try {
                        int deleted = jdbc.update(CLEANUP_SQL, new MapSqlParameterSource().addValue("ttl", ttl));
                        if (deleted > 0 && log.isDebugEnabled()) {
                            log.debug("cleaned {} expired idempotent records (distributed)", deleted);
                        }
                    } catch (Exception e) {
                        log.warn("idempotent distributed cleanup error", e);
                    }
                }
            );
            return new Object();
        }
    }

}
