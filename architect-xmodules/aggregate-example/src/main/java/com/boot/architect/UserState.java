package com.boot.architect;


import com.cloud.arch.enums.Value;

public enum UserState implements Value<Integer> {
    INVALID(0, "无效"),
    NORMAL(1, "正常"),
    LOCKED(2, "冻结");

    private final Integer state;
    private final String  remark;

    UserState(Integer state, String remark) {
        this.state  = state;
        this.remark = remark;
    }


    /**
     * 获取枚举变量唯一值
     */
    @Override
    public Integer value() {
        return this.state;
    }

    /**
     * 枚举值描述
     */
    @Override
    public String label() {
        return this.remark;
    }
}
