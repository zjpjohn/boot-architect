package com.cloud.arch.core;

public interface INamedFunction {

    /**
     * 是否前置执行
     */
    default boolean beforeInvoke() {
        return false;
    }

    /**
     * 函数名称
     */
    String name();

    /**
     * 获取函数操作值
     *
     * @param value 参数
     */
    String apply(String value);

}
