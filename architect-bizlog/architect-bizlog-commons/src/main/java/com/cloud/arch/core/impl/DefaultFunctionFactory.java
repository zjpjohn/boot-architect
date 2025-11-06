package com.cloud.arch.core.impl;


import com.cloud.arch.core.IFunctionFactory;
import com.cloud.arch.core.LogFunctionContainer;

import java.util.Optional;

public class DefaultFunctionFactory implements IFunctionFactory {

    private final LogFunctionContainer container;

    public DefaultFunctionFactory(LogFunctionContainer container) {
        this.container = container;
    }

    /**
     * 指定名称函数计算
     *
     * @param name  函数名称
     * @param value 参数值
     */
    @Override
    public String apply(String name, String value) {
        return Optional.ofNullable(container.getFunction(name)).map(v -> v.apply(value)).orElse(value);
    }

    /**
     * 是否前置执行
     *
     * @param name 函数名称
     */
    @Override
    public boolean isBefore(String name) {
        return container.isBefore(name);
    }

}
