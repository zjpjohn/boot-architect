package com.cloud.arch.rocket.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MsgSendResult 发送结果")
class MsgSendResultTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("msgId 和 topic 正确赋值")
        void shouldSetFields() {
            MsgSendResult result = new MsgSendResult();
            result.setMsgId("msg-001");
            result.setTopic("order-topic");

            assertThat(result.getMsgId()).isEqualTo("msg-001");
            assertThat(result.getTopic()).isEqualTo("order-topic");
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("新实例字段为 null")
        void shouldHaveNullFieldsByDefault() {
            MsgSendResult result = new MsgSendResult();
            assertThat(result.getMsgId()).isNull();
            assertThat(result.getTopic()).isNull();
        }
    }
}
