package com.cloud.arch.web.enums;

import com.cloud.arch.enums.Value;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class EnumValueSerializer extends JsonSerializer<Value<? extends Comparable<?>>> {

    @Override
    public void serialize(Value value, JsonGenerator generator, SerializerProvider serializerProvider)
            throws IOException {
        generator.writeObject(value.toMap());
    }
}
