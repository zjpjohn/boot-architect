package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.arch.page.Pager;

public class PagerConverter {

    public static <T> Pager<T> convert(Page<T> page) {
        Pager<T> pager = new Pager<>();
        pager.setTotal(page.getTotal());
        pager.setPageSize(page.getSize());
        pager.setRecords(page.getRecords());
        pager.setCurrent(page.getCurrent());
        return pager;
    }
}
