package com.cloud.arch.web.converter.factory;

import com.cloud.arch.web.converter.ConvertParseException;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeConverterFactory {

    private static final String TIME_FORMAT_PATTERN = "HH:mm:ss";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String DATE_TIME_PATTERN   = "yyyy-MM-dd HH:mm:ss";

    private TimeConverterFactory() {
    }

    private static String ofDefault(String format, String defaultVal) {
        if (StringUtils.hasText(format)) {
            return format;
        }
        return defaultVal;
    }

    public static class LocalDateConverter implements Converter<String, LocalDate> {

        private final DateTimeFormatter formatter;
        private final String            pattern;

        public LocalDateConverter(WebMvcProperties.Format format) {
            this.pattern   = ofDefault(format.getDate(), DATE_FORMAT_PATTERN);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }

        @Override
        public LocalDate convert(String source) {
            try {
                return LocalDate.parse(source, formatter);
            } catch (Exception error) {
                throw new ConvertParseException("日期格式因为'" + this.pattern + "'.");
            }
        }
    }

    public static class LocalTimeConverter implements Converter<String, LocalTime> {
        private final DateTimeFormatter formatter;
        private final String            pattern;

        public LocalTimeConverter(WebMvcProperties.Format format) {
            this.pattern   = ofDefault(format.getTime(), TIME_FORMAT_PATTERN);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }

        @Override
        public LocalTime convert(String source) {
            try {
                return LocalTime.parse(source, formatter);
            } catch (Exception error) {
                throw new ConvertParseException("时间格式因为'" + this.pattern + "'.");
            }
        }
    }

    public static class LocalDateTimeConverter implements Converter<String, LocalDateTime> {
        private final DateTimeFormatter formatter;
        private final String            pattern;

        public LocalDateTimeConverter(WebMvcProperties.Format format) {
            this.pattern   = ofDefault(format.getDateTime(), DATE_TIME_PATTERN);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }

        @Override
        public LocalDateTime convert(String source) {
            try {
                return LocalDateTime.parse(source, formatter);
            } catch (Exception error) {
                throw new ConvertParseException("日期时间格式因为'" + this.pattern + "'.");
            }
        }
    }

    public static class DateTimeConverter implements Converter<String, Date> {

        private final String pattern;

        public DateTimeConverter(WebMvcProperties.Format format) {
            this.pattern = ofDefault(format.getDateTime(), DATE_TIME_PATTERN);
        }

        @Override
        public Date convert(String source) {
            try {
                return DateUtils.parseDate(source, pattern);
            } catch (Exception e) {
                throw new ConvertParseException("日期格式应为'" + pattern + "'.");
            }
        }
    }

}
