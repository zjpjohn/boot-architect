package com.cloud.arch.event.core.publish;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventMessage 事件消息")
class EventMessageTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            EventMessage msg = new EventMessage();
            msg.setName("order-topic");
            msg.setFilter("created");
            msg.setDelay(5000L);
            msg.setKey("msg-123");
            msg.setData("{\"orderId\":1}");

            assertThat(msg.getName()).isEqualTo("order-topic");
            assertThat(msg.getFilter()).isEqualTo("created");
            assertThat(msg.getDelay()).isEqualTo(5000L);
            assertThat(msg.getKey()).isEqualTo("msg-123");
            assertThat(msg.getData()).isEqualTo("{\"orderId\":1}");
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("新实例所有字段为 null")
        void shouldHaveNullFieldsByDefault() {
            EventMessage msg = new EventMessage();
            assertThat(msg.getName()).isNull();
            assertThat(msg.getFilter()).isNull();
            assertThat(msg.getDelay()).isNull();
            assertThat(msg.getKey()).isNull();
            assertThat(msg.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段的实例 equals 相等")
        void shouldEqualWithSameFields() {
            EventMessage m1 = new EventMessage();
            m1.setName("t");
            EventMessage m2 = new EventMessage();
            m2.setName("t");
            assertThat(m1).isEqualTo(m2);
        }

        @Test
        @DisplayName("不同 name 的实例不相等")
        void shouldNotEqualWithDifferentName() {
            EventMessage m1 = new EventMessage();
            m1.setName("t1");
            EventMessage m2 = new EventMessage();
            m2.setName("t2");
            assertThat(m1).isNotEqualTo(m2);
        }
    }
}
