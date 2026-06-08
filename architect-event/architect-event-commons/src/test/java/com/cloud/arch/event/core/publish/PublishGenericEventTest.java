package com.cloud.arch.event.core.publish;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@DisplayName("PublishGenericEvent 通用事件实现")
class PublishGenericEventTest {

    @Nested
    @DisplayName("构造函数链")
    class Constructors {

        @Test
        @DisplayName("双参构造默认 filter 为空字符串")
        void shouldDefaultFilterToEmpty() {
            PublishGenericEvent event = new PublishGenericEvent("data", "topic");
            assertThat(event.filter()).isEmpty();
            assertThat(event.shardingKey()).isEmpty();
            assertThat(event.bizGroup()).isEmpty();
            assertThat(event.delay()).isZero();
            assertThat(event.timeUnit()).isEqualTo(TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("全参构造所有字段正确")
        void shouldSetAllFields() {
            PublishGenericEvent event = new PublishGenericEvent(
                    "data", "topic", "tag", "shard-1", "order",
                    30L, TimeUnit.MINUTES);

            assertThat(event.event()).isEqualTo("data");
            assertThat(event.name()).isEqualTo("topic");
            assertThat(event.filter()).isEqualTo("tag");
            assertThat(event.shardingKey()).isEqualTo("shard-1");
            assertThat(event.bizGroup()).isEqualTo("order");
            assertThat(event.delay()).isEqualTo(30L);
            assertThat(event.timeUnit()).isEqualTo(TimeUnit.MINUTES);
        }
    }

    @Nested
    @DisplayName("GenericEvent 接口方法")
    class InterfaceMethods {

        @Test
        @DisplayName("event() 返回构造时传入的值")
        void shouldReturnEvent() {
            PublishGenericEvent event = new PublishGenericEvent("hello", "topic");
            assertThat(event.event()).isEqualTo("hello");
        }

        @Test
        @DisplayName("name() 返回构造时传入的值")
        void shouldReturnName() {
            PublishGenericEvent event = new PublishGenericEvent("data", "my-topic");
            assertThat(event.name()).isEqualTo("my-topic");
        }
    }
}
