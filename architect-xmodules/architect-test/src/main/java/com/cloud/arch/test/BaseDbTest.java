package com.cloud.arch.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL 集成测试基类，启动 Testcontainers MySQL 容器。
 * 容器在 {@code @BeforeAll} 启动，{@code @AfterAll} 停止，类内所有测试共享同一实例。
 *
 * <pre>{@code
 * class JdbcRepositoryIT extends BaseDbTest {
 *     @Test
 *     void shouldPersistAndQuery() {
 *         // getJdbcUrl(), getUsername(), getPassword() 获取连接信息
 *     }
 * }
 * }</pre>
 */
public abstract class BaseDbTest {

    private static final Logger log = LoggerFactory.getLogger(BaseDbTest.class);

    private static MySQLContainer<?> mysqlContainer;

    @BeforeAll
    static void startMysql() {
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8"))
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        mysqlContainer.start();
        log.info("MySQL container started at {}", mysqlContainer.getJdbcUrl());
    }

    @AfterAll
    static void stopMysql() {
        if (mysqlContainer != null) {
            mysqlContainer.stop();
        }
    }

    public static String getJdbcUrl() {
        return mysqlContainer.getJdbcUrl();
    }

    public static String getUsername() {
        return mysqlContainer.getUsername();
    }

    public static String getPassword() {
        return mysqlContainer.getPassword();
    }

    public static String getDriverClassName() {
        return mysqlContainer.getDriverClassName();
    }
}
