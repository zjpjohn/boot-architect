package com.cloud.arch.web.extension;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.cloud.arch.enums.Value;

import java.lang.reflect.Type;

public class EnumValueWriter implements ObjectWriter<Object> {
    @Override
    public void write(JSONWriter writer, Object value, Object name, Type type, long feature) {
        writer.write(((Value<?>)value).toMap());
    }

}
