package com.cloud.arch.web.mask;


import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;


public class MaskSerializer extends ValueSerializer<String> {
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
    public void serialize(String value, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String masked = MaskUtils.mask(value, type, ratio, maskChar, minLength);
        gen.writeString(masked);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctx, BeanProperty property) {
        if (property != null) {
            Mask anno = property.getAnnotation(Mask.class);
            if (anno != null) {
                return new MaskSerializer(anno);
            }
        }
        return ctx.findValueSerializer(String.class);
    }

}
