package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.function.BiFunction;

public class PageWrapper<T> {

    private final Page<T>    page;
    private final Wrapper<T> wrapper;

    public PageWrapper(Page<T> page, Wrapper<T> wrapper) {
        this.page    = page;
        this.wrapper = wrapper;
    }

    public Page<T> pageList(BiFunction<Page<T>, Wrapper<T>, Page<T>> loader) {
        return loader.apply(page, wrapper);
    }

}
