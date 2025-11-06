package com.cloud.arch.infrast.persist.enums;

import com.cloud.arch.enums.Value;
import com.cloud.arch.mybatis.annotations.TypeHandler;
import com.cloud.arch.mybatis.core.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@TypeHandler(type = Type.ENUM)
public enum Gender implements Value<Integer> {

    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer value;
    private final String  label;

    @Override
    public Integer value() {
        return this.value;
    }

    @Override
    public String label() {
        return this.label;
    }

}
