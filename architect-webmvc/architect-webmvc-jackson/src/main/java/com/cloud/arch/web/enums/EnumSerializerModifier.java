package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class EnumSerializerModifier extends BeanSerializerModifier {

    @Override
    public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
        BeanDescription beanDesc, JsonSerializer<?> serializer) {
        Class<?> type = valueType.getRawClass();
        if (Value.class.isAssignableFrom(type)) {
            return new EnumValueSerializer();
        }
        return super.modifyEnumSerializer(config, valueType, beanDesc, serializer);
    }
}
