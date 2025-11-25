package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.function.BiFunction;

public record PagerWrapper<T>(Pager<T> page, Wrapper<T> wrapper) {

    public Pager<T> pageList(BiFunction<Pager<T>, Wrapper<T>, Pager<T>> loader) {
        return loader.apply(page, wrapper);
    }

}
