package com.cloud.arch.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompareResult 比较结果")
class CompareResultTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("added/removed 字段正确赋值")
        void shouldSetFields() {
            Set<String> added = Set.of("a", "b");
            Set<String> removed = Set.of("c");
            CompareResult<String> result = new CompareResult<>(added, removed);

            assertThat(result.getAdded()).containsExactlyInAnyOrder("a", "b");
            assertThat(result.getRemoved()).containsExactly("c");
        }

        @Test
        @DisplayName("空集合同样支持")
        void shouldSupportEmptySets() {
            CompareResult<Integer> result = new CompareResult<>(Collections.emptySet(), Collections.emptySet());
            assertThat(result.getAdded()).isEmpty();
            assertThat(result.getRemoved()).isEmpty();
        }
    }
}
