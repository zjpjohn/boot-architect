package com.cloud.arch.aggregate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
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

    /**
     * 判断比较结果整体为空
     */
    public boolean empty() {
        return (added == null || added.isEmpty()) && (removed == null || removed.isEmpty());
    }

}
