package com.cloud.arch.hotkey.convert;


import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.LongAdder;

public class LongAdderSerializer implements ObjectWriter<LongAdder> {

    @Override
    public void write(JSONWriter writer, Object value, Object fieldName, Type type, long feature) {
        if(value instanceof Long){

        }
        LongAdder adder = (LongAdder) value;
        writer.writeAny(JSONObject.of("value", adder.longValue()));
    }

}
