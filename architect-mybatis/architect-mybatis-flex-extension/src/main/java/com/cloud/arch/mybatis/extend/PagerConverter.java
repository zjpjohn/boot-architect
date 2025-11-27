package com.cloud.arch.mybatis.extend;

import com.cloud.arch.page.Pager;
import com.mybatisflex.core.paginate.Page;

public class PagerConverter {

    /**
     * 转换为外部分页
     */
    public static <T> Pager<T> convert(Page<T> page) {
        Pager<T> pager = new Pager<>();
        pager.setTotal(page.getTotalRow());
        pager.setPageSize(page.getPageSize());
        pager.setCurrent(page.getPageNumber());
        pager.setRecords(page.getRecords());
        return pager;
    }

}
