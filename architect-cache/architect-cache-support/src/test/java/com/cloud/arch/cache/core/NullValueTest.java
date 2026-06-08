package com.cloud.arch.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NullValue 缓存空值哨兵")
class NullValueTest {

    @Test
    @DisplayName("INSTANCE 单例非 null")
    void shouldHaveNonNullInstance() {
        assertThat(NullValue.INSTANCE).isNotNull();
    }

    @Test
    @DisplayName("INSTANCE 是 NullValue 类型")
    void shouldBeNullValueType() {
        assertThat(NullValue.INSTANCE).isInstanceOf(NullValue.class);
    }

    @Test
    @DisplayName("toString 包含类名")
    void shouldContainClassNameInToString() {
        assertThat(NullValue.INSTANCE.toString()).contains("NullValue");
    }
}
