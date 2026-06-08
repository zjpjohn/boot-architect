package com.cloud.arch.rocket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DelayMessage 延迟消息")
class DelayMessageTest {

    @Nested
    @DisplayName("泛型构造")
    class GenericConstruction {

        @Test
        @DisplayName("序列化类型参数正确存储")
        void shouldStoreSerializableBody() {
            Set<Long> delivers = new HashSet<>();
            delivers.add(3L);
            DelayMessage<String> msg = new DelayMessage<>("topic", "tag", "body", delivers, "biz-1");

            assertThat(msg.getTopic()).isEqualTo("topic");
            assertThat(msg.getTag()).isEqualTo("tag");
            assertThat(msg.getBody()).isEqualTo("body");
            assertThat(msg.getDelivers()).containsExactly(3L);
            assertThat(msg.getBizKey()).isEqualTo("biz-1");
        }

        @Test
        @DisplayName("无参构造默认字段为 null")
        void shouldHaveNullFieldsByDefault() {
            DelayMessage<?> msg = new DelayMessage<>();
            assertThat(msg.getTopic()).isNull();
            assertThat(msg.getTag()).isNull();
            assertThat(msg.getBody()).isNull();
            assertThat(msg.getDelivers()).isNull();
            assertThat(msg.getBizKey()).isNull();
        }
    }
}
