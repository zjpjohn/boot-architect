package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;


public class EnumValueSerializer extends ValueSerializer<Value<? extends Comparable<?>>> {

    @Override
    public void serialize(Value<? extends Comparable<?>> value, JsonGenerator generator, SerializationContext ctx) throws JacksonException {
        generator.writePOJO(value.toMap());
    }

}
