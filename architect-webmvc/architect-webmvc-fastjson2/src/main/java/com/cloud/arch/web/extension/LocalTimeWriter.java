package com.cloud.arch.web.extension;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeWriter implements ObjectWriter<Object> {

    private final DateTimeFormatter formatter;

    public LocalTimeWriter(String format) {
        this.formatter = DateTimeFormatter.ofPattern(format);
    }

    @Override
    public void write(JSONWriter jsonWriter, Object value, Object name, Type type, long l) {
        if (value == null) {
            jsonWriter.writeNull();
            return;
        }
        jsonWriter.writeString(((LocalTime)value).format(formatter));
    }

}
