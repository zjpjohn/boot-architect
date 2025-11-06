package com.cloud.arch.web.converter;

import com.cloud.arch.web.converter.factory.EnumConverterFactory;
import com.cloud.arch.web.converter.factory.TimeConverterFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public record WebConverterConfigurer(WebMvcProperties properties) implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        //非‘application/json’请求枚举参数转换
        registry.addConverterFactory(new EnumConverterFactory());
        //date日期格式化转化
        registry.addConverter(new TimeConverterFactory.DateTimeConverter(properties.getFormat()));
        //LocalDate、LocalTime、LocalDateTime格式化转换
        registry.addConverter(new TimeConverterFactory.LocalDateConverter(properties.getFormat()));
        registry.addConverter(new TimeConverterFactory.LocalTimeConverter(properties.getFormat()));
        registry.addConverter(new TimeConverterFactory.LocalDateTimeConverter(properties.getFormat()));
    }

}
