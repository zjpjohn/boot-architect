package com.cloud.arch.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;


public class JsonUtils {

    public static <T> T toBean(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static String toJson(Object bean) {
        return JSON.toJSONString(bean);
    }

    public static <V> V readValue(File file, Class<V> clazz) {
        try {
            return JSON.parseObject(new FileInputStream(file), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <V> V readValue(URL url, Class<V> clazz) {
        return JSON.parseObject(url, clazz);
    }

    public static <V> V readValue(InputStream stream, Class<V> clazz) {
        return JSON.parseObject(stream, clazz);
    }

    public static <V> List<V> readList(File file, Class<V> clazz) {
        try {
            return JSON.parseArray(new FileInputStream(file)).toJavaList(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <V> List<V> readList(URL url, Class<V> clazz) {
        return JSON.parseArray(url).toJavaList(clazz);
    }

    public static <V> List<V> readList(InputStream stream, Class<V> clazz) {
        return JSON.parseArray(stream).toJavaList(clazz);
    }

    public static <K, V> Map<K, V> readMap(File file) {
        try {
            return JSON.parseObject(new FileInputStream(file))
                    .to(new TypeReference<Map<K, V>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> readMap(InputStream stream) {
        try {
            return JSON.parseObject(stream).to(new TypeReference<Map<K, V>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> readMap(URL url) {
        return JSON.parseObject(url).to(new TypeReference<Map<K, V>>() {
        });
    }

    public static <K, V> Map<K, V> toMap(String json) {
        return JSON.parseObject(json, new TypeReference<Map<K, V>>() {
        });
    }


    public static <T> List<T> toList(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    /**
     * 获取指定字段的json字符串
     */
    public static String toJson(String json, String name) {
        return toValue(json, name, JSONObject::getString).orElse(null);
    }

    /**
     * 获取json字符串中指定字段的值
     *
     * @param json json字符串
     * @param name 字段名称
     */
    public static String toString(String json, String name) {
        return toValue(json, name, JSONObject::getString).orElse("");
    }


    /**
     * 获取指定字段的long型值
     */
    public static Long toLong(String json, String name) {
        return toValue(json, name, JSONObject::getLong).orElse(null);
    }

    /**
     * 获取指定字段的int型值
     */
    public static Integer toInt(String json, String name) {
        return toValue(json, name, JSONObject::getInteger).orElse(null);
    }

    /**
     * 获取json中的布尔值
     */
    public static boolean toBoolean(String json, String name) {
        return toValue(json, name, JSONObject::getBoolean).orElse(false);
    }

    private static <T> Optional<T> toValue(String json, String name, BiFunction<JSONObject, String, T> function) {
        Preconditions.checkArgument(StringUtils.isNotBlank(json), "json must not be empty...");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name must not be empty...");
        return Optional.ofNullable(JSON.parseObject(json))
                .map(jsonObject -> function.apply(jsonObject, name));
    }

    /**
     * json 转复杂对象
     */
    public static <T> T toValue(String json, TypeReference<T> typeReference) {
        try {
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
