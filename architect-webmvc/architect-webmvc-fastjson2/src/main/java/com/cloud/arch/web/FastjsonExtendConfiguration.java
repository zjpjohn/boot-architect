package com.cloud.arch.web;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.alibaba.fastjson2.support.spring6.webservlet.view.FastJsonJsonView;
import com.cloud.arch.enums.Value;
import com.cloud.arch.web.extension.*;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Configuration
@SuppressWarnings({"rawtypes", "unchecked"})
@EnableConfigurationProperties(FastjsonProperties.class)
public class FastjsonExtendConfiguration implements WebMvcConfigurer, InitializingBean {

    public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    @Resource
    private FastjsonProperties properties;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter  = new FastJsonHttpMessageConverter();
        FastJsonConfig               jsonConfig = new FastJsonConfig();
        jsonConfig.setJSONB(properties.isJsonb());
        jsonConfig.setWriteContentLength(properties.isWriteContentLength());
        jsonConfig.setWriterFeatures(writerFeatures());
        jsonConfig.setReaderFeatures(readFeatures());
        converter.setFastJsonConfig(jsonConfig);
        converter.setDefaultCharset(properties.getCharset());
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        converters.add(0, converter);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        FastJsonJsonView jsonView   = new FastJsonJsonView();
        FastJsonConfig   jsonConfig = new FastJsonConfig();
        jsonConfig.setWriterFeatures(writerFeatures());
        jsonConfig.setReaderFeatures(readFeatures());
        jsonView.setFastJsonConfig(jsonConfig);
        registry.enableContentNegotiation(jsonView);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerLocalWriters();
        this.registerEnumWriters();
    }

    private void registerLocalWriters() {
        String dateFormat =
            Optional.ofNullable(properties.getDateFormat()).filter(StringUtils::isNotBlank).orElse(DATE_FORMAT);
        String[] formats = this.formats(dateFormat);
        JSON.register(LocalDateTime.class, new LocalDateTimeWriter(dateFormat));
        JSON.register(LocalDate.class, new LocalDateWriter(formats[0]));
        JSON.register(LocalTime.class, new LocalTimeWriter(formats[1]));
    }

    private void registerEnumWriters() {
        Iterable<Class<? extends Value>> classes = ClassIndex.getSubclasses(Value.class);
        StreamSupport.stream(classes.spliterator(), false)
                     .filter(clazz -> !clazz.isInterface() && clazz.isEnum())
                     .forEach(this::registerEnum);
    }

    private <K extends Comparable<K>, T extends Value<K>> void registerEnum(Class<T> type) {
        JSON.registerIfAbsent(type, new EnumValueWriter());
        JSON.registerIfAbsent(type, new EnumValueReader<>(type), true);
    }

    private JSONWriter.Feature[] writerFeatures() {
        List<JSONWriter.Feature> features =
            Lists.newArrayList(JSONWriter.Feature.WriteLongAsString, JSONWriter.Feature.BrowserCompatible, JSONWriter.Feature.BrowserSecure);
        if (!properties.isIgnoreNull()) {
            features.add(JSONWriter.Feature.WriteNulls);
        }
        return features.toArray(new JSONWriter.Feature[0]);
    }

    private JSONReader.Feature[] readFeatures() {
        ArrayList<JSONReader.Feature> features =
            Lists.newArrayList(JSONReader.Feature.FieldBased, JSONReader.Feature.IgnoreAutoTypeNotMatch, JSONReader.Feature.UseDefaultConstructorAsPossible, JSONReader.Feature.AllowUnQuotedFieldNames);
        if (properties.isIgnoreNull()) {
            features.add(JSONReader.Feature.IgnoreSetNullValue);
        }
        return features.toArray(new JSONReader.Feature[0]);
    }

    private String[] formats(String format) {
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
