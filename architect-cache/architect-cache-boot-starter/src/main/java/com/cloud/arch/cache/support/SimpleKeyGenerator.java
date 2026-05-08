package com.cloud.arch.cache.support;

import java.lang.reflect.Method;

/**
 * 默认缓存 Key 生成器，单参直接返回参数值，多参返回 {@link SimpleKey} 实例，无参返回 {@link SimpleKey#EMPTY}
 */
public class SimpleKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generate(params);
    }

    private Object generate(Object[] params) {
        if (params == null || params.length == 0) {
            return SimpleKey.EMPTY;
        }
        if (params.length == 1) {
            Object param = params[0];
            if (param != null && !param.getClass().isArray()) {
                return param;
            }
        }
        return new SimpleKey(params);
    }
}
