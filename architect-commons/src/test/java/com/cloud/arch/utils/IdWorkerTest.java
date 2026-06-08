package com.cloud.arch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IdWorker ID 生成器")
class IdWorkerTest {

    @Nested
    @DisplayName("nextId 基本行为")
    class NextId {

        @Test
        @DisplayName("生成正数 ID")
        void shouldReturnPositiveId() {
            long id = IdWorker.nextId();
            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("批量生成 10000 个 ID 无重复")
        void shouldGenerateUniqueIds() {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 10000; i++) {
                long id = IdWorker.nextId();
                assertThat(ids.add(id)).as("Duplicate ID: %d", id).isTrue();
            }
        }

        @Test
        @DisplayName("趋势递增（后生成的 ID 大于先生成的）")
        void shouldBeTrendIncreasing() {
            long prev = IdWorker.nextId();
            // 等待时间推进确保序列号不会绕过
            for (int i = 0; i < 100; i++) {
                long curr = IdWorker.nextId();
                assertThat(curr).isGreaterThan(prev);
                prev = curr;
            }
        }
    }

    @Nested
    @DisplayName("uuid 基本行为")
    class Uuid {

        @Test
        @DisplayName("返回非空字符串")
        void shouldReturnNonBlankString() {
            String uuid = IdWorker.uuid();
            assertThat(uuid).isNotBlank();
        }

        @Test
        @DisplayName("批量生成 10000 个 uuid 无重复")
        void shouldGenerateUniqueUuids() {
            Set<String> uuids = new HashSet<>();
            for (int i = 0; i < 10000; i++) {
                String uuid = IdWorker.uuid();
                assertThat(uuids.add(uuid)).as("Duplicate uuid: %s", uuid).isTrue();
            }
        }
    }
}
