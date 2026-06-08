package com.cloud.arch.rocket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MessageModel 消息模型")
class MessageModelTest {

    @Nested
    @DisplayName("枚举值")
    class Values {

        @Test
        @DisplayName("BROADCASTING → \"BROADCASTING\"")
        void shouldBroadcasting() {
            assertThat(MessageModel.BROADCASTING.getModel()).isEqualTo("BROADCASTING");
        }

        @Test
        @DisplayName("CLUSTERING → \"CLUSTERING\"")
        void shouldClustering() {
            assertThat(MessageModel.CLUSTERING.getModel()).isEqualTo("CLUSTERING");
        }
    }
}
