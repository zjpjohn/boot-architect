package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cloud.arch.page.Page;

import java.util.function.BiFunction;

public record PagerWrapper<T>(Pager<T> page, Wrapper<T> wrapper) {

    /**
     * 返回mybatis-plus内部定义的Page对象
     */
    public Pager<T> pagerList(BiFunction<Pager<T>, Wrapper<T>, Pager<T>> loader) {
        return loader.apply(page, wrapper);
    }

    /**
     * 转换为通用的Page对象
     */
    public Page<T> pageList(BiFunction<Pager<T>, Wrapper<T>, Pager<T>> loader) {
        return loader.apply(page, wrapper).transform();
    }

}
