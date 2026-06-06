package com.cloud.arch.web.mask;

import com.alibaba.fastjson2.filter.ValueFilter;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class MaskValueFilter implements ValueFilter {

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Mask>> cache = new ConcurrentHashMap<>();

    @Override
    public Object apply(Object object, String name, Object value) {
        if (!(value instanceof String)) return value;
        Mask mask = getMaskAnnotation(object.getClass(), name);
        if (mask != null) {
            return MaskUtils.mask((String) value, mask.type(), mask.ratio(), mask.masker(), mask.minLength());
        }
        return value;
    }

    private Mask getMaskAnnotation(Class<?> clazz, String fieldName) {
        return cache.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).computeIfAbsent(fieldName, fn -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                return field.getAnnotation(Mask.class);
            } catch (NoSuchFieldException e) {
                return null;
            }
        });
    }
}
