package com.cloud.arch.core;

public interface IOperatorFunction {

    /**
     * 获取当前操作用户信息
     */
    Operator operator(String operatorId);

}
