package com.cloud.arch.web.enums;

import com.cloud.arch.enums.EnumValue;
import com.cloud.arch.enums.Value;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumValueDeserializer<K extends Comparable<K>, V extends Value<K>> extends JsonDeserializer<V>
    implements ContextualDeserializer {

    private final EnumValue<K, V> enumValue;

    public EnumValueDeserializer(Class<V> type) {
        this.enumValue = new EnumValue<>(type);
    }

    @Override
    public V deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        K        value    = (K)getValue(jsonNode);
        if (value != null) {
            return this.enumValue.get(value);
        }
        return null;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty beanProperty)
        throws JsonMappingException {
        return new EnumValueDeserializer(context.getContextualType().getRawClass());
    }

    private Comparable<?> getValue(JsonNode node) {
        if (node.isFloat()) {
            return node.floatValue();
        }
        if (node.isInt()) {
            return node.intValue();
        }
        if (node.isDouble()) {
            return node.doubleValue();
        }
        if (node.isLong()) {
            return node.longValue();
        }
        if (node.isShort()) {
            return node.shortValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        return null;
    }
}
