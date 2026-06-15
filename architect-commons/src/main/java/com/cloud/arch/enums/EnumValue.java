package com.cloud.arch.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class EnumValue<K extends Comparable<K>, V extends Value<K>> {

    private final Class<V>  type;
    private final ValueType valueType;
    private final Map<K, V> enumMap;

    public EnumValue(Class<V> type) {
        this.type = type;
        V[] values = type.getEnumConstants();
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("enum values collection must not be null.");
        }
        String typeName = values[0].value().getClass().getName();
        this.valueType = ValueType.of(typeName);
        this.enumMap = Arrays.stream(values).collect(Collectors.toMap(Value::value, Function.identity()));
    }

    public V get(K key) {
        return this.enumMap.get(key);
    }

    public V of(String source) {
        K key = valueType.toValue(source);
        return this.enumMap.get(key);
    }

    public List<K> values() {
        return this.enumMap.values().stream().map(Value::value).toList();
    }

}
