package com.cloud.arch.event.core.publish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.arch.event.annotations.Publish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringValueResolver;

import java.util.concurrent.TimeUnit;

@DisplayName("PublishMetadata 发布元数据")
class PublishMetadataTest {

    @Nested
    @DisplayName("本地事件")
    class Local {

        @Test
        @DisplayName("name 为空 → local=true")
        void shouldBeLocalWhenNameBlank() {
            Publish ann = mock(Publish.class);
            when(ann.name()).thenReturn("");
            PublishMetadata meta = new PublishMetadata(ann, mock(StringValueResolver.class));
            assertThat(meta.isLocal()).isTrue();
        }

        @Test
        @DisplayName("本地事件不解析 topic/filter/delay")
        void shouldNotResolveForLocal() {
            Publish ann = mock(Publish.class);
            when(ann.name()).thenReturn("");
            PublishMetadata meta = new PublishMetadata(ann, mock(StringValueResolver.class));
            assertThat(meta.getName()).isNull();
            assertThat(meta.getFilter()).isNull();
            assertThat(meta.getDelay()).isNull();
        }
    }

    @Nested
    @DisplayName("远程事件")
    class Remote {

        private final StringValueResolver resolver = mock(StringValueResolver.class);

        @BeforeEach
        void setUp() {
            when(resolver.resolveStringValue("order-topic")).thenReturn("order-topic");
            when(resolver.resolveStringValue("created")).thenReturn("created");
        }

        @Test
        @DisplayName("name 非空 → local=false")
        void shouldNotBeLocal() {
            Publish ann = mock(Publish.class);
            when(ann.name()).thenReturn("order-topic");
            when(ann.filter()).thenReturn("");
            when(ann.timeUnit()).thenReturn(TimeUnit.SECONDS);
            PublishMetadata meta = new PublishMetadata(ann, resolver);
            assertThat(meta.isLocal()).isFalse();
        }

        @Test
        @DisplayName("解析 topic 和 filter")
        void shouldResolveTopicAndFilter() {
            Publish ann = mock(Publish.class);
            when(ann.name()).thenReturn("order-topic");
            when(ann.filter()).thenReturn("created");
            when(ann.bizGroup()).thenReturn("order");
            when(ann.timeUnit()).thenReturn(TimeUnit.SECONDS);
            PublishMetadata meta = new PublishMetadata(ann, resolver);
            assertThat(meta.getName()).isEqualTo("order-topic");
            assertThat(meta.getFilter()).isEqualTo("created");
            assertThat(meta.getBizGroup()).isEqualTo("order");
        }

        @Test
        @DisplayName("delay 转换为毫秒")
        void shouldConvertDelayToMillis() {
            Publish ann = mock(Publish.class);
            when(ann.name()).thenReturn("order-topic");
            when(ann.filter()).thenReturn("");
            when(ann.delay()).thenReturn(5L);
            when(ann.timeUnit()).thenReturn(TimeUnit.SECONDS);
            PublishMetadata meta = new PublishMetadata(ann, resolver);
            assertThat(meta.getDelay()).isEqualTo(5000L);
        }
    }
}
