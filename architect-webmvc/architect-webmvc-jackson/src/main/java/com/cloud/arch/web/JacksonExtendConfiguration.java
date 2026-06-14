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
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonExtendConfiguration {

    public static final String PATTERN_FORMAT = "yyyy/MM/dd HH:mm:ss";

    @Bean
    public JsonMapperBuilderCustomizer jackson2ObjectCustomizer(JacksonProperties props) {
        String format = Optional.ofNullable(props.getDateFormat())
                                .filter(StringUtils::isNotBlank)
                                .orElse(PATTERN_FORMAT);
        String[]          formats       = formatSplit(props.getDateFormat());
        DateTimeFormatter formatter     = DateTimeFormatter.ofPattern(format);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(formats[0]);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(formats[1]);
        return (JsonMapper.Builder builder) -> {
            // 自定义序列化和反序列化
            SimpleModule module = new SimpleModule();
            module.setDeserializerModifier(new EnumDeserializerModifier())
                  .setSerializerModifier(new EnumSerializerModifier())
                  .addSerializer(Long.class, ToStringSerializer.instance)
                  .addSerializer(BigInteger.class, ToStringSerializer.instance)
                  .addSerializer(BigDecimal.class, ToStringSerializer.instance)
                  .addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter))
                  .addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter))
                  .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
                  .addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter))
                  .addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter))
                  .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            builder.addModule(module)
                   .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                   .changeDefaultPropertyInclusion((v) -> v.withValueInclusion(JsonInclude.Include.NON_NULL)
                                                           .withContentInclusion(JsonInclude.Include.NON_NULL));
        };
    }

    private String[] formatSplit(String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("datetime format must not be blank.");
        }
        String[] formats = null;
        if (format.contains(" ")) {
            formats = format.split("\\s+");
        } else if (format.contains("T")) {
            formats = format.split("T");
        }
        if (formats == null || formats.length > 2) {
            throw new IllegalArgumentException(String.format("malformed datetime format '%s'.", format));
        }
        return formats;
    }

}
