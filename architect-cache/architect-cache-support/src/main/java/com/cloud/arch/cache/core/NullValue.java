package com.cloud.arch.cache.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class NullValue {

    public static final Object INSTANCE = new NullValue();

    @Override
    public String toString() {
        return "NullValue{}";
    }
}
