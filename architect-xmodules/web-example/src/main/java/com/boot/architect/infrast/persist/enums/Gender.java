package com.boot.architect.infrast.persist.enums;

import com.cloud.arch.enums.Value;
import com.cloud.arch.web.dict.Dictionary;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Dictionary(name = "gender",type = "int",remark = "性别")
public enum Gender implements Value<Integer> {
    FEMALE(1, "女"),
    MALE(2, "男");

    private final Integer value;
    private final String  name;

    @Override
    public Integer value() {
        return this.value;
    }

    @Override
    public String label() {
        return this.name;
    }
}
