package com.cloud.arch.support.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.arch.annotations.OperateLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnnotationMetadata 注解元数据")
class AnnotationMetadataTest {

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("正常构造 → 解析字段")
        void shouldParseAnnotationFields() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.group()).thenReturn("bizGroup");
            when(ann.tenant()).thenReturn("{#tenant}");
            when(ann.bizNo()).thenReturn("{#order.id}");
            when(ann.success()).thenReturn("创建成功");
            when(ann.failure()).thenReturn("");
            when(ann.operator()).thenReturn("{#user.id}");
            when(ann.detail()).thenReturn("{#order.detail}");
            when(ann.condition()).thenReturn("");

            AnnotationMetadata meta = new AnnotationMetadata(ann);
            assertThat(meta.getGroup()).isEqualTo("bizGroup");
            assertThat(meta.getBizNo()).isEqualTo("{#order.id}");
            assertThat(meta.getSuccess()).isEqualTo("创建成功");
            assertThat(meta.getOperator()).isEqualTo("{#user.id}");
        }

        @Test
        @DisplayName("success 和 failure 都为空 → 抛异常")
        void shouldThrowWhenBothSuccessAndFailureBlank() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("");
            when(ann.failure()).thenReturn("");
            assertThatThrownBy(() -> new AnnotationMetadata(ann))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("success");
        }

        @Test
        @DisplayName("只有 success → 正常")
        void shouldAcceptWhenOnlySuccessSet() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("操作成功");
            when(ann.failure()).thenReturn("");
            when(ann.bizNo()).thenReturn("");
            when(ann.tenant()).thenReturn("");
            when(ann.detail()).thenReturn("");
            when(ann.operator()).thenReturn("");
            when(ann.condition()).thenReturn("");
            AnnotationMetadata meta = new AnnotationMetadata(ann);
            assertThat(meta.getSuccess()).isEqualTo("操作成功");
        }

        @Test
        @DisplayName("只有 failure → 正常")
        void shouldAcceptWhenOnlyFailureSet() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("");
            when(ann.failure()).thenReturn("操作失败");
            when(ann.bizNo()).thenReturn("");
            when(ann.tenant()).thenReturn("");
            when(ann.detail()).thenReturn("");
            when(ann.operator()).thenReturn("");
            when(ann.condition()).thenReturn("");
            AnnotationMetadata meta = new AnnotationMetadata(ann);
            assertThat(meta.getFail()).isEqualTo("操作失败");
        }
    }

    @Nested
    @DisplayName("getSpelTemplates()")
    class SpelTemplates {

        @Test
        @DisplayName("succeed=true → 包含 success 模板")
        void shouldIncludeSuccessTemplate() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("成功");
            when(ann.failure()).thenReturn("失败");
            when(ann.bizNo()).thenReturn("{#id}");
            when(ann.tenant()).thenReturn("");
            when(ann.detail()).thenReturn("");
            when(ann.operator()).thenReturn("");
            when(ann.condition()).thenReturn("");

            AnnotationMetadata meta = new AnnotationMetadata(ann);
            assertThat(meta.getSpelTemplates(true)).contains("成功");
            assertThat(meta.getSpelTemplates(true)).doesNotContain("失败");
        }

        @Test
        @DisplayName("succeed=false → 包含 failure 模板")
        void shouldIncludeFailureTemplate() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("成功");
            when(ann.failure()).thenReturn("失败");
            when(ann.bizNo()).thenReturn("{#id}");
            when(ann.tenant()).thenReturn("");
            when(ann.detail()).thenReturn("");
            when(ann.operator()).thenReturn("");
            when(ann.condition()).thenReturn("");

            AnnotationMetadata meta = new AnnotationMetadata(ann);
            assertThat(meta.getSpelTemplates(false)).contains("失败");
            assertThat(meta.getSpelTemplates(false)).doesNotContain("成功");
        }

        @Test
        @DisplayName("空 tenant/detail/operator/condition 不加入模板列表")
        void shouldExcludeBlankOptionalFields() {
            OperateLog ann = mock(OperateLog.class);
            when(ann.success()).thenReturn("成功");
            when(ann.failure()).thenReturn("");
            when(ann.bizNo()).thenReturn("{#id}");
            when(ann.tenant()).thenReturn("");
            when(ann.detail()).thenReturn("");
            when(ann.operator()).thenReturn("");
            when(ann.condition()).thenReturn("");

            AnnotationMetadata meta = new AnnotationMetadata(ann);
            // 只有 bizNo + success
            assertThat(meta.getSpelTemplates(true)).hasSize(2);
            assertThat(meta.getSpelTemplates(true)).containsExactly("{#id}", "成功");
        }
    }
}
