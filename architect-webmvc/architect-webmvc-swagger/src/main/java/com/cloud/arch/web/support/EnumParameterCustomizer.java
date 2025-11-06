package com.cloud.arch.web.support;

import com.cloud.arch.enums.Value;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class EnumParameterCustomizer implements ParameterCustomizer, EnumValueCustomizer {

    @Override
    public Parameter customize(Parameter parameterModel, MethodParameter methodParameter) {
        Class<?> parameterType = methodParameter.getParameterType();
        if (!Enum.class.isAssignableFrom(parameterType) || !Value.class.isAssignableFrom(parameterType)) {
            return parameterModel;
        }
        Schema<Object>                       schema = new Schema<>();
        Triple<List<Object>, String, String> value  = enumInfo(parameterType);
        parameterModel.setDescription(value.getRight());
        schema.setEnum(value.getLeft());
        String type = value.getMiddle();
        if (StringUtils.hasText(type)) {
            schema.setType(type);
        }
        parameterModel.setSchema(schema);
        return parameterModel;
    }

}
