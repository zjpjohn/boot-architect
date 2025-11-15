package com.cloud.arch.aggregate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class CompareResult<T> {

    /**
     * 新增的数据
     */
    private Set<T> added;
    /**
     * 删除的数据
     */
    private Set<T> removed;

}
