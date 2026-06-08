package com.cloud.arch.operate.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@DisplayName("DefaultOperatorResolver 默认操作者解析器")
class DefaultOperatorResolverTest {

    @Nested
    @DisplayName("resolve()")
    class Resolve {

        @Test
        @DisplayName("空列表 → 返回空 Map")
        void shouldReturnEmptyMapForEmptyList() {
            DefaultOperatorResolver resolver = new DefaultOperatorResolver();
            Map<Long, String> result = resolver.resolve(Collections.emptyList());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("非空列表 → 返回空 Map（默认实现）")
        void shouldReturnEmptyMapForNonEmptyList() {
            DefaultOperatorResolver resolver = new DefaultOperatorResolver();
            List<Long> ids = Arrays.asList(1L, 2L, 3L);
            Map<Long, String> result = resolver.resolve(ids);
            assertThat(result).isEmpty();
        }
    }
}
