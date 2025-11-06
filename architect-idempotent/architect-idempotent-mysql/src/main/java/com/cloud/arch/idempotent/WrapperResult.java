package com.cloud.arch.idempotent;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrapperResult {

    public static final WrapperResult EMPTY = new WrapperResult(null);

    private Object result;

    public Object getValue(Class<?> type) {
        return Optional.ofNullable(result).map(v -> JSON.to(type, v)).orElse(null);
    }

}
