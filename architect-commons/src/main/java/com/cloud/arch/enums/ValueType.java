package com.cloud.arch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public enum ValueType {

    BYTE(Byte.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Byte.valueOf(value).compareTo((Byte) source.value());
        }

        @Override
        public int compareTo(Comparable source, String target) {
            return Byte.valueOf(target).compareTo((Byte) source);
        }

        @Override
        public Byte toValue(String source) {
            return Byte.valueOf(source);
        }
    },
    SHORT(Short.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Short.valueOf(value).compareTo((Short) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return Short.valueOf(target).compareTo((Short) source);
        }

        @Override
        public Short toValue(String source) {
            return Short.valueOf(source);
        }
    },
    INT(Integer.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Integer.valueOf(value).compareTo((Integer) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return Integer.valueOf(target).compareTo((Integer) source);
        }

        @Override
        public Integer toValue(String source) {
            return Integer.valueOf(source);
        }
    },
    LONG(Long.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Long.valueOf(value).compareTo((Long) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return Long.valueOf(target).compareTo((Long) source);
        }

        @Override
        public Long toValue(String source) {
            return Long.valueOf(source);
        }
    },
    FLOAT(Float.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Float.valueOf(value).compareTo((Float) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return Float.valueOf(target).compareTo((Float) source);
        }

        @Override
        public Float toValue(String source) {
            return Float.valueOf(source);
        }
    },
    STRING(String.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return value.compareTo((String) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return target.compareTo((String) source);
        }

        @Override
        public String toValue(String value) {
            return value;
        }
    },
    DOUBLE(Double.class.getName()) {
        @Override
        public int compareTo(String value, Value source) {
            return Double.valueOf(value).compareTo((Double) source.value());
        }

        @Override
        public int compareTo(Comparable<?> source, String target) {
            return Double.valueOf(target).compareTo((Double) source);
        }

        @Override
        public Double toValue(String source) {
            return Double.valueOf(source);
        }
    };
    private final String name;

    public abstract <V extends Comparable<V>> int compareTo(String value, Value<V> source);

    public abstract int compareTo(Comparable<?> source, String target);

    public abstract <V extends Comparable<V>> V toValue(String source);

    public static ValueType of(String type) {
        return Arrays.stream(values())
                     .filter(v -> v.name.equals(type))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException(String.format("不支持参数类型[%s].", type)));
    }

}
