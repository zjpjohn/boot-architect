package com.cloud.arch.web.support;

import com.cloud.arch.enums.Value;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.BeanUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public interface EnumValueCustomizer {

    /**
     * 获取枚举信息
     */
    default Triple<List<Object>, String, String> enumInfo(Class<?> type) {
        Value[] enums = (Value[]) type.getEnumConstants();
        List<Object> values = Arrays.stream(enums)
                                    .filter(Objects::nonNull)
                                    .map(Value::value)
                                    .collect(Collectors.toList());
        String description = Arrays.stream(enums)
                                   .map(e -> e.value().toString() + "-" + e.label())
                                   .collect(Collectors.joining(";", "枚举值:", ""));
        String typeName = "";
        if (enums.length > 0) {
            Object        value    = enums[0].value();
            PrimitiveType fromType = PrimitiveType.fromType(value.getClass());
            if (fromType != null) {
                typeName = fromType.getCommonName();
            }
        }
        return Triple.of(values, typeName, description);
    }


    /**
     * 获取枚举类的schema
     */
    default Schema<Object> ofSchema(Class<?> type, Schema<?> source) {
        PrimitiveType fromType = PrimitiveType.fromType(type);
        Schema        schema   = fromType != null ? fromType.createProperty() : new ObjectSchema();
        String        format   = schema.getFormat();
        BeanUtils.copyProperties(source, schema);
        return schema.format(format);
    }
}
