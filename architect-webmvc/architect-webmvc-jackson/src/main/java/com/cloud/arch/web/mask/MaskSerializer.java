package com.cloud.arch.web.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class MaskSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private MaskType type;
    private double   ratio;
    private char     maskChar;
    private int      minLength;

    public MaskSerializer() {
    }

    public MaskSerializer(Mask anno) {
        this.type = anno.type();
        this.ratio = anno.ratio();
        this.maskChar = anno.masker();
        this.minLength = anno.minLength();
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String masked = MaskUtils.mask(value, type, ratio, maskChar, minLength);
        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            Mask anno = property.getAnnotation(Mask.class);
            if (anno != null) {
                return new MaskSerializer(anno);
            }
        }
        return prov.findValueSerializer(String.class, property);
    }

}
