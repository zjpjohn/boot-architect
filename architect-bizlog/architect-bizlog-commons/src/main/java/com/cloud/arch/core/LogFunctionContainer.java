package com.cloud.arch.core;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class LogFunctionContainer {

    private final Map<String, INamedFunction> cachedFunctions = Maps.newHashMap();

    public LogFunctionContainer(List<INamedFunction> functions) {
        functions.stream()
                .filter(v -> StringUtils.isNotBlank(v.name()))
                .forEach(v -> cachedFunctions.put(v.name(), v));
    }

    /**
     * 获取指定名称的函数操作
     *
     * @param name 函数名称
     */
    public INamedFunction getFunction(String name) {
        return cachedFunctions.get(name);
    }

    /**
     * 判断指定名称函数是否前置执行
     *
     * @param name 函数名称
     */
    public boolean isBefore(String name) {
        INamedFunction function = this.cachedFunctions.get(name);
        return function != null && function.beforeInvoke();
    }

}
