package com.boot.architect.application.query.dto;

import com.cloud.arch.enums.Value;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum Gender implements Value<Integer> {
    MALE("男", 1),
    FEMALE("女", 2);

    private final String  name;
    private final Integer value;

    @Override
    public Integer value() {
        return this.value;
    }

    @Override
    public String label() {
        return this.name;
    }
}
