package com.cloud.arch.core.impl;


import com.cloud.arch.core.IOperatorFunction;
import com.cloud.arch.core.Operator;

public class DefaultOperatorFunction implements IOperatorFunction {

    /**
     * 获取当前操作用户信息
     */
    @Override
    public Operator operator(String operatorId) {
        return new Operator("0", "system-user");
    }

}
