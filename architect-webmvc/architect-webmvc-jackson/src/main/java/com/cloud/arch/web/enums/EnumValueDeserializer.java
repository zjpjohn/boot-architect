package com.cloud.arch.web.enums;

import com.cloud.arch.enums.EnumValue;
import com.cloud.arch.enums.Value;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumValueDeserializer<K extends Comparable<K>, V extends Value<K>> extends ValueDeserializer<V> {

    private final EnumValue<K, V> enumValue;

    public EnumValueDeserializer(Class<V> type) {
        this.enumValue = new EnumValue<>(type);
    }

    @Override
    public V deserialize(JsonParser jsonParser, DeserializationContext ctx) throws JacksonException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        K        value    = (K) getValue(jsonNode);
        if (value != null) {
            return this.enumValue.get(value);
        }
        return null;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty beanProperty) {
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
        if (node.isString()) {
            return node.asString();
        }
        return null;
    }

}
