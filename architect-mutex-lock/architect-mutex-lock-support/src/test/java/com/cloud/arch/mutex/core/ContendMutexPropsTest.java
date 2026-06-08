package com.cloud.arch.mutex.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ContendMutexProps 竞争锁配置")
class ContendMutexPropsTest {

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("initialDelay 默认 0 秒")
        void shouldDefaultInitialDelayToZero() {
            ContendMutexProps props = new ContendMutexProps();
            assertThat(props.getInitialDelay()).isEqualTo(Duration.ofSeconds(0));
        }

        @Test
        @DisplayName("ttl 默认 10 秒")
        void shouldDefaultTtlTo10s() {
            ContendMutexProps props = new ContendMutexProps();
            assertThat(props.getTtl()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("transition 默认 6 秒")
        void shouldDefaultTransitionTo6s() {
            ContendMutexProps props = new ContendMutexProps();
            assertThat(props.getTransition()).isEqualTo(Duration.ofSeconds(6));
        }
    }

    @Nested
    @DisplayName("自定义配置")
    class Custom {

        @Test
        @DisplayName("可设置全部参数")
        void shouldSetAllParams() {
            ContendMutexProps props = new ContendMutexProps(
                    Duration.ofSeconds(1), Duration.ofSeconds(30), Duration.ofSeconds(20));

            assertThat(props.getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
            assertThat(props.getTtl()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.getTransition()).isEqualTo(Duration.ofSeconds(20));
        }
    }
}
