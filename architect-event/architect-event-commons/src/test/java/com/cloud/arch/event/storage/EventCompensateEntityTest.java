package com.cloud.arch.event.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventCompensateEntity 补偿实体")
class EventCompensateEntityTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            EventCompensateEntity entity = new EventCompensateEntity();
            entity.setId(1L);
            entity.setEventId(100L);
            entity.setShardingKey("db01");
            entity.setStartTime(1700000000000L);
            entity.setTaken(1500L);
            entity.setFailedMsg("connection timeout");
            entity.setGmtCreate(java.time.LocalDateTime.of(2024, 1, 1, 0, 0));

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getEventId()).isEqualTo(100L);
            assertThat(entity.getShardingKey()).isEqualTo("db01");
            assertThat(entity.getStartTime()).isEqualTo(1700000000000L);
            assertThat(entity.getTaken()).isEqualTo(1500L);
            assertThat(entity.getFailedMsg()).isEqualTo("connection timeout");
            assertThat(entity.getGmtCreate()).isEqualTo("2024-01-01T00:00");
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("新实例所有字段为 null")
        void shouldHaveNullFieldsByDefault() {
            EventCompensateEntity entity = new EventCompensateEntity();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getEventId()).isNull();
            assertThat(entity.getShardingKey()).isNull();
            assertThat(entity.getStartTime()).isNull();
            assertThat(entity.getTaken()).isNull();
            assertThat(entity.getFailedMsg()).isNull();
        }
    }
}
