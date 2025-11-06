package com.cloud.arch.redis;

import org.atteo.classindex.ClassIndex;

import java.util.stream.StreamSupport;

public class CodecTypeUtils {

    private CodecTypeUtils() {
        throw new UnsupportedOperationException("not support invoke.");
    }

    public static Class<?>[] typList() {
        Iterable<Class<?>> iterable = ClassIndex.getAnnotated(CodecTypeMarker.class);
        return StreamSupport.stream(iterable.spliterator(), false)
                            .filter(clazz -> !clazz.isInterface())
                            .toArray(Class<?>[]::new);
    }

}
