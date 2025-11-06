package com.cloud.arch.enums;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 统一枚举接口
 */
public interface Value<T extends Comparable<T>> {

    /**
     * 枚举值
     */
    T value();

    /**
     * 枚举描述信息
     */
    default String label() {
        return "";
    }

    /**
     * 判断与给定值是否相等
     */
    default boolean equal(T value) {
        return Objects.equals(value(), value);
    }

    /**
     * 枚举信息转换成Map
     */
    default Map<String, Object> toMap() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("value", this.value());
        result.put("label", this.label());
        return result;
    }

    /**
     * 查找指定值的枚举
     */
    static <K extends Comparable<K>, V extends Value<K>> Optional<V> ofNullable(K key, Class<V> clazz) {
        V[] constants = clazz.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(constants).filter(e -> e.value().compareTo(key) == 0).findFirst();
    }

    /**
     * 查找指定值的枚举
     */
    static <K extends Comparable<K>, V extends Value<K>> V valueOf(K key, Class<V> clazz) {
        return ofNullable(key, clazz).orElse(null);
    }

}
