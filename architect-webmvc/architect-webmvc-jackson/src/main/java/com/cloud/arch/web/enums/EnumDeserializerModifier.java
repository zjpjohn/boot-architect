package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumDeserializerModifier extends ValueDeserializerModifier {

    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type, BeanDescription.Supplier beanDesc, ValueDeserializer<?> deserializer) {
        Class<?> typeClazz = type.getRawClass();
        if (typeClazz.isEnum() && Value.class.isAssignableFrom(typeClazz)) {
            return new EnumValueDeserializer(typeClazz);
        }
        return super.modifyEnumDeserializer(config, type, beanDesc, deserializer);
    }

}
