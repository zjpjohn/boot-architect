package com.cloud.arch.page;

import java.util.List;
import java.util.function.Function;

public class PageWrapper {

    private PageCondition condition;
    private Integer       total;
    private boolean       needCount;

    private PageWrapper() {
    }

    public static PageWrapper wrap(PageCondition condition) {
        PageWrapper cnt = new PageWrapper();
        cnt.condition = condition;
        cnt.total     = 0;
        cnt.needCount = false;
        return cnt;
    }

    public static PageWrapper wrap(PageCondition condition, Function<PageCondition, Integer> counter) {
        PageWrapper cnt = new PageWrapper();
        cnt.total     = counter.apply(condition);
        cnt.condition = condition;
        cnt.needCount = true;
        return cnt;
    }

    public <T> Page<T> query(Function<PageCondition, List<T>> dataFunc) {
        Page<T> page = new Page<>();
        page.setCurrent(this.condition.getPage());
        page.setPageSize(this.condition.getLimit());
        if (!this.needCount) {
            List<T> records = dataFunc.apply(condition);
            page.setRecords(records);
            page.setSize(records.size());
            return page;
        }
        page.setTotal(this.total);
        if (this.total > 0 && condition.getOffset() <= this.total) {
            List<T> records = dataFunc.apply(condition);
            page.setRecords(records);
            page.setSize(records.size());
        }
        return page;
    }

}
