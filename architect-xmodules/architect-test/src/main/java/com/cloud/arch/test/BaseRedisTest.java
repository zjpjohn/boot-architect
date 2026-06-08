package com.cloud.arch.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 集成测试基类，启动 Testcontainers Redis 容器。
 * 容器在 {@code @BeforeAll} 启动，{@code @AfterAll} 停止，类内所有测试共享同一实例。
 *
 * <pre>{@code
 * class RedisRemoteCacheIT extends BaseRedisTest {
 *     @Test
 *     void shouldStoreAndRetrieveValue() {
 *         // getRedisHost(), getRedisPort() 获取连接信息
 *     }
 * }
 * }</pre>
 */
public abstract class BaseRedisTest {

    private static final Logger log = LoggerFactory.getLogger(BaseRedisTest.class);
    private static final int REDIS_PORT = 6379;

    private static GenericContainer<?> redisContainer;

    @BeforeAll
    static void startRedis() {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);
        redisContainer.start();
        log.info("Redis container started at {}:{}", redisContainer.getHost(), getRedisPort());
    }

    @AfterAll
    static void stopRedis() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    public static String getRedisHost() {
        return redisContainer.getHost();
    }

    public static int getRedisPort() {
        return redisContainer.getMappedPort(REDIS_PORT);
    }

    public static String getRedisAddress() {
        return "redis://" + getRedisHost() + ":" + getRedisPort();
    }
}
