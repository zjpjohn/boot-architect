package com.cloud.arch.hotkey.utils;

import com.cloud.arch.hotkey.convert.LongAdderDelegate;
import com.google.common.collect.Maps;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;

public class ProtostuffUtils {

    private final static DefaultIdStrategy ID_STRATEGY = ((DefaultIdStrategy) RuntimeEnv.ID_STRATEGY);

    /**
     * 缓存schema
     */
    private final static Map<Class<?>, Schema<?>> schemaCache = Maps.newConcurrentMap();

    /**
     * LongAdder序列化与反序列化
     */
    static {
        ID_STRATEGY.registerDelegate(new LongAdderDelegate());
    }

    /**
     * 序列化对象
     *
     * @param obj 待序列化对象
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T>     clazz  = (Class<T>) obj.getClass();
        Schema<T>    schema = getSchema(clazz);
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化
     *
     * @param data  字节数组数据
     * @param clazz 指定类型
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        Schema<T> schema = getSchema(clazz);
        T         obj    = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }


    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(clazz, ID_STRATEGY);
            if (schema != null) {
                schemaCache.put(clazz, schema);
            }
        }
        return schema;
    }
}
