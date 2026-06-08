package com.cloud.arch.rocket.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DelayMetadata 延迟元数据")
class SenderMetadataDelayMetadataTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("index/deliver/timeUnit 正确赋值")
        void shouldSetFields() {
            SenderMetadata.DelayMetadata delay = new SenderMetadata.DelayMetadata(2, true, TimeUnit.MINUTES);
            assertThat(delay.getIndex()).isEqualTo(2);
            assertThat(delay.isDeliver()).isTrue();
            assertThat(delay.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        }
    }

    @Nested
    @DisplayName("collection 标志")
    class CollectionFlag {

        @Test
        @DisplayName("默认 isCollection 为 null")
        void shouldDefaultCollectionToNull() {
            SenderMetadata.DelayMetadata delay = new SenderMetadata.DelayMetadata(0, false, TimeUnit.SECONDS);
            assertThat(delay.isCollection()).isNull();
        }

        @Test
        @DisplayName("setCollection(true) → isCollection=true")
        void shouldSetCollectionTrue() {
            SenderMetadata.DelayMetadata delay = new SenderMetadata.DelayMetadata(0, false, TimeUnit.SECONDS);
            delay.setCollection(true);
            assertThat(delay.isCollection()).isTrue();
        }

        @Test
        @DisplayName("setCollection(false) → isCollection=false")
        void shouldSetCollectionFalse() {
            SenderMetadata.DelayMetadata delay = new SenderMetadata.DelayMetadata(0, false, TimeUnit.SECONDS);
            delay.setCollection(false);
            assertThat(delay.isCollection()).isFalse();
        }
    }
}
