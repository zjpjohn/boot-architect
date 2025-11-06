package com.cloud.arch.web.extension;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeWriter implements ObjectWriter<Object> {

    private final DateTimeFormatter formatter;

    public LocalDateTimeWriter(String format) {
        this.formatter = DateTimeFormatter.ofPattern(format);
    }

    @Override
    public void write(JSONWriter jsonWriter, Object value, Object name, Type type, long feature) {
        if (value == null) {
            jsonWriter.writeNull();
            return;
        }
        jsonWriter.writeString(((LocalDateTime)value).format(formatter));
    }

}
