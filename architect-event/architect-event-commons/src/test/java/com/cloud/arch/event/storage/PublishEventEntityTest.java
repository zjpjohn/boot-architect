package com.cloud.arch.event.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.arch.event.core.publish.EventState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PublishEventEntity 发布事件实体")
class PublishEventEntityTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认构造 fields 为 null")
        void shouldHaveNullFieldsByDefault() {
            PublishEventEntity entity = new PublishEventEntity();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getName()).isNull();
        }

        @Test
        @DisplayName("id 构造函数正确赋值")
        void shouldSetId() {
            PublishEventEntity entity = new PublishEventEntity(123L);
            assertThat(entity.getId()).isEqualTo(123L);
        }
    }

    @Nested
    @DisplayName("build() 转换为 EventMessage")
    class Build {

        @Test
        @DisplayName("所有字段映射到 EventMessage")
        void shouldMapAllFields() {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setId(100L);
            entity.setName("order-topic");
            entity.setFilter("created");
            entity.setDelay(5000L);
            entity.setEvent("{\"data\":1}");
            entity.setState(EventState.INITIALIZED);

            assertThat(entity.build())
                    .returns("order-topic", com.cloud.arch.event.core.publish.EventMessage::getName)
                    .returns("created", com.cloud.arch.event.core.publish.EventMessage::getFilter)
                    .returns(5000L, com.cloud.arch.event.core.publish.EventMessage::getDelay)
                    .returns("{\"data\":1}", com.cloud.arch.event.core.publish.EventMessage::getData)
                    .returns("100", com.cloud.arch.event.core.publish.EventMessage::getKey);
        }
    }

    @Nested
    @DisplayName("getEventState()")
    class GetEventState {

        @Test
        @DisplayName("INITIALIZED → 0")
        void shouldReturnStateZero() {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setState(EventState.INITIALIZED);
            assertThat(entity.getEventState()).isEqualTo(0);
        }

        @Test
        @DisplayName("SUCCEEDED → 1")
        void shouldReturnStateOne() {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setState(EventState.SUCCEEDED);
            assertThat(entity.getEventState()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Version 接口")
    class VersionInterface {

        @Test
        @DisplayName("setVersion/getVersion 正确")
        void shouldSetAndGetVersion() {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setVersion(3);
            assertThat(entity.getVersion()).isEqualTo(3);
        }
    }
}
