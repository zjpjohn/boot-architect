package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
        BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        Class<?> typeClazz = type.getRawClass();
        if (typeClazz.isEnum() && Value.class.isAssignableFrom(typeClazz)) {
            return new EnumValueDeserializer(typeClazz);
        }
        return super.modifyEnumDeserializer(config, type, beanDesc, deserializer);
    }

}
