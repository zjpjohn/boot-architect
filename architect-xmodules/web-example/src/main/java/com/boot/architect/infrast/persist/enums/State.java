package com.boot.architect.infrast.persist.enums;

import com.cloud.arch.enums.Value;
import com.cloud.arch.web.dict.Dictionary;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Dictionary(name = "state", type = "int", remark = "公共状态")
public enum State implements Value<Integer> {
    CREATED(1, "已创建"),
    USING(2, "使用中"),
    FORBIDDEN(3, "已禁用");

    private final Integer state;
    private final String  name;

    @Override
    public Integer value() {
        return this.state;
    }

    @Override
    public String label() {
        return this.name;
    }
}
