package com.cloud.arch.transaction.core;

import com.alibaba.fastjson2.JSON;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Optional;

@Slf4j
@NoArgsConstructor
public class AsyncTxParams extends LinkedHashMap<Integer, Object> {

    public AsyncTxParams(Object[] arguments) {
        if (arguments != null && arguments.length > 0) {
            for (int i = 0; i < arguments.length; i++) {
                put(i, arguments[i]);
            }
        }
    }

    /**
     * 根据方法参数类型及位置从json中解析出参数集合
     */
    public Object[] jsonArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        if (types.length == 0) {
            return new Object[0];
        }
        Object[] arguments = new Object[types.length];
        int      length    = Math.min(types.length, this.size());
        for (int i = 0; i < length; i++) {
            arguments[i] = this.getArgument(i, types[i]);
        }
        return arguments;
    }

    /**
     * 获取指定位置的参数
     *
     * @param index 方法参数位置下标
     * @param type  参数类型
     */
    private Object getArgument(int index, Class<?> type) {
        Object value = get(index);
        return Optional.ofNullable(value).map(v -> JSON.to(type, v)).orElse(null);
    }

}
