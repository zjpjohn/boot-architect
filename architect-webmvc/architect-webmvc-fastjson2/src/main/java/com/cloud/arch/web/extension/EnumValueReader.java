package com.cloud.arch.web.extension;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.cloud.arch.enums.EnumValue;
import com.cloud.arch.enums.Value;
import com.cloud.arch.enums.ValueType;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class EnumValueReader<K extends Comparable<K>, V extends Value<K>> implements ObjectReader<Object> {

    private final EnumValue<K, V> enumValue;

    public EnumValueReader(Class<V> type) {
        this.enumValue = new EnumValue<>(type);
    }

    @Override
    public Object readObject(JSONReader reader, Type type, Object fieldName, long feature) {
        ValueType valueType = enumValue.getValueType();
        return switch (valueType) {
            case BYTE -> ofValue(reader, JSONReader::readInt8);
            case SHORT -> ofValue(reader, JSONReader::readInt16);
            case INT -> ofValue(reader, JSONReader::readInt32);
            case LONG -> ofValue(reader, JSONReader::readInt64);
            case FLOAT -> ofValue(reader, JSONReader::readFloat);
            case DOUBLE -> ofValue(reader, JSONReader::readDouble);
            case STRING -> ofValue(reader, JSONReader::readString);
        };
    }

    private V ofValue(JSONReader reader, Function<JSONReader, Comparable<?>> function) {
        return Optional.ofNullable(function.apply(reader)).map(v -> (K) v).map(enumValue::get).orElse(null);
    }
}
