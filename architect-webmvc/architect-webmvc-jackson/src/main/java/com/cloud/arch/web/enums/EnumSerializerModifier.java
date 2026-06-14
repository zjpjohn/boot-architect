package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class EnumSerializerModifier extends ValueSerializerModifier {

    @Override
    public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
        Class<?> type = valueType.getRawClass();
        if (Value.class.isAssignableFrom(type)) {
            return new EnumValueSerializer();
        }
        return super.modifyEnumSerializer(config, valueType, beanDesc, serializer);
    }
}
