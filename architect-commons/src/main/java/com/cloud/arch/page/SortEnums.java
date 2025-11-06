package com.cloud.arch.page;

public enum SortEnums {
    //最新创建
    LATEST_CREATE(1),
    //最近更新
    LATEST_MODIFY(2);

    private final Integer sort;

    SortEnums(Integer sort) {
        this.sort = sort;
    }

    public Integer getSort() {
        return sort;
    }
}
