package com.cloud.arch.event.reparation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReparationRequest 修复请求")
class ReparationRequestTest {

    @Nested
    @DisplayName("validate() 验证")
    class Validate {

        @Test
        @DisplayName("eventId/topic/body 均有效 → 通过")
        void shouldPassWhenAllRequiredFieldsSet() {
            ReparationRequest req = new ReparationRequest();
            req.setEventId(1L);
            req.setTopic("order-topic");
            req.setBody("{\"data\":1}");
            assertThat(req.validate()).isTrue();
        }

        @Test
        @DisplayName("eventId 为 null → 不通过")
        void shouldFailWhenEventIdNull() {
            ReparationRequest req = new ReparationRequest();
            req.setTopic("order-topic");
            req.setBody("{\"data\":1}");
            assertThat(req.validate()).isFalse();
        }

        @Test
        @DisplayName("topic 为空 → 不通过")
        void shouldFailWhenTopicBlank() {
            ReparationRequest req = new ReparationRequest();
            req.setEventId(1L);
            req.setTopic("");
            req.setBody("{\"data\":1}");
            assertThat(req.validate()).isFalse();
        }

        @Test
        @DisplayName("body 为空 → 不通过")
        void shouldFailWhenBodyBlank() {
            ReparationRequest req = new ReparationRequest();
            req.setEventId(1L);
            req.setTopic("order-topic");
            req.setBody("");
            assertThat(req.validate()).isFalse();
        }
    }

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("bizGroup 默认空字符串")
        void shouldDefaultBizGroupToEmpty() {
            ReparationRequest req = new ReparationRequest();
            assertThat(req.getBizGroup()).isEmpty();
        }
    }
}
