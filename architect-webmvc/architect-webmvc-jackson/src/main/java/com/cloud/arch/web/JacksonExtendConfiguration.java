package com.cloud.arch.web;

import com.cloud.arch.web.enums.EnumDeserializerModifier;
import com.cloud.arch.web.enums.EnumSerializerModifier;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;

@Configuration
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonExtendConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer jackson2ObjectCustomizer(JacksonProperties props) {
        return (JsonMapper.Builder builder) -> {
            // 自定义序列化和反序列化
            SimpleModule module = new SimpleModule();
            module.setDeserializerModifier(new EnumDeserializerModifier())
                  .setSerializerModifier(new EnumSerializerModifier())
                  .addSerializer(Long.class, ToStringSerializer.instance)
                  .addSerializer(BigInteger.class, ToStringSerializer.instance)
                  .addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.addModule(module)
                   .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                   .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                   .changeDefaultPropertyInclusion((v) -> v.withContentInclusion(JsonInclude.Include.NON_NULL));
        };

    }


}
