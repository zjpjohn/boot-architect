package com.cloud.arch.core;

public interface IFunctionFactory {

    /**
     * 指定名称函数计算
     *
     * @param name  函数名称
     * @param value 参数值
     */
    String apply(String name, String value);

    /**
     * 是否前置执行
     *
     * @param name 函数名称
     */
    boolean isBefore(String name);

}
