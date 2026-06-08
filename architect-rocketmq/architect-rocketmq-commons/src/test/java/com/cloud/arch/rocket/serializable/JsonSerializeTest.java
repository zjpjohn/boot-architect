package com.cloud.arch.rocket.serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JsonSerialize JSON 序列化")
class JsonSerializeTest {

    private final JsonSerialize serialize = new JsonSerialize();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Person implements Serializable {
        private String name;
        private int age;
    }

    @Nested
    @DisplayName("往返序列化")
    class RoundTrip {

        @Test
        @DisplayName("对象序列化后反序列化 → 等于原对象")
        void shouldRoundTrip() {
            Person person = new Person("Alice", 30);
            byte[] bytes = serialize.serialize(person);
            Person restored = serialize.deSerialize(bytes, Person.class);
            assertThat(restored.getName()).isEqualTo("Alice");
            assertThat(restored.getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("字符串序列化往返")
        void shouldRoundTripString() {
            byte[] bytes = serialize.serialize("hello world");
            String restored = serialize.deSerialize(bytes, String.class);
            assertThat(restored).isEqualTo("hello world");
        }

        @Test
        @DisplayName("null 值序列化")
        void shouldSerializeNull() {
            byte[] bytes = serialize.serialize(null);
            assertThat(bytes).isNotNull();
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ErrorHandling {

        @Test
        @DisplayName("错误类型反序列化 → RuntimeException")
        void shouldThrowOnMismatch() {
            byte[] bytes = serialize.serialize(new Person("Bob", 25));
            // 用错误类型反序列化
            byte[] corrupted = new byte[]{1, 2, 3};
            assertThatThrownBy(() -> serialize.deSerialize(corrupted, Person.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("反序列化");
        }
    }
}
