package com.cloud.arch.web;

import com.cloud.arch.web.enums.EnumDeserializerModifier;
import com.cloud.arch.web.enums.EnumSerializerModifier;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonExtendConfiguration {

    public static final String PATTERN_FORMAT = "yyyy/MM/dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectCustomizer(JacksonProperties props) {
        String format =
            Optional.ofNullable(props.getDateFormat()).filter(StringUtils::isNotBlank).orElse(PATTERN_FORMAT);
        String[]          formats       = formatSplit(props.getDateFormat());
        DateTimeFormatter formatter     = DateTimeFormatter.ofPattern(format);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(formats[0]);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(formats[1]);
        return (Jackson2ObjectMapperBuilder builder) -> {
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            // 序列化
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.serializerByType(Date.class, new DateSerializer(false, new SimpleDateFormat(format)));
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            // 反序列化
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            builder.deserializerByType(Date.class, new DateDeserializers.DateDeserializer(DateDeserializers.DateDeserializer.instance, new SimpleDateFormat(format), format));
            // 自定义枚举序列化or反序列化处理
            builder.modules(new SimpleModule().setDeserializerModifier(new EnumDeserializerModifier())
                                              .setSerializerModifier(new EnumSerializerModifier()));
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
