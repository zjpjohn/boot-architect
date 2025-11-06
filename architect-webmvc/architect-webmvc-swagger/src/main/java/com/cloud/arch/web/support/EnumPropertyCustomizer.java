package com.cloud.arch.web.support;

import com.cloud.arch.enums.Value;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class EnumPropertyCustomizer implements PropertyCustomizer, EnumValueCustomizer {
    @Override
    public Schema customize(Schema property, AnnotatedType type) {
        if (type.getType() instanceof SimpleType fieldType) {
            Class<?> fieldClazz = fieldType.getRawClass();
            if (Enum.class.isAssignableFrom(fieldClazz) && Value.class.isAssignableFrom(fieldClazz)) {
                Schema<Object>                       schema = ofSchema(fieldClazz, property);
                Triple<List<Object>, String, String> value  = enumInfo(fieldClazz);
                schema.setEnum(value.getLeft());
                //设置枚举值类型
                String typeName = value.getMiddle();
                if (StringUtils.hasText(typeName)) {
                    schema.setType(typeName);
                }
                //设置title
                String description = value.getRight();
                String title       = property.getTitle();
                title = StringUtils.hasText(title) ? title + "(" + description + ")" : description;
                schema.setTitle(title);

                //设置description
                if (StringUtils.hasText(property.getDescription())) {
                    description = property.getDescription() + " (" + description + ")";
                }
                description = description
                              + "\n备注说明：前端传参时，只需传递枚举值即可；后端返回的枚举数据格式{'value':枚举值,'label':'枚举说明'}";
                schema.setDescription(description);
                return schema;
            }
        }
        return property;
    }
}
