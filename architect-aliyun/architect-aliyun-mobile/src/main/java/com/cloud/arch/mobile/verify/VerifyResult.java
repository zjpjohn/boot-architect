package com.cloud.arch.mobile.verify;


import com.cloud.arch.enums.Value;

import java.util.Arrays;

public enum VerifyResult implements Value<String> {

    PASS("PASS", "一致"),
    REJECT("REJECT", "不一致"),
    UNKNOWN("UNKNOWN", "无法判断");

    private final String value;
    private final String remark;

    VerifyResult(String value, String remark) {
        this.value  = value;
        this.remark = remark;
    }

    public String getValue() {
        return value;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * 获取枚举变量唯一值
     */
    @Override
    public String value() {
        return value;
    }

    /**
     * 枚举值描述
     */
    @Override
    public String label() {
        return remark;
    }

    public static VerifyResult of(String value) {
        return Arrays.stream(values())
                     .filter(e -> e.value.equals(value))
                     .findFirst()
                     .orElseThrow(() -> new RuntimeException("未知的认证结果."));
    }
}
