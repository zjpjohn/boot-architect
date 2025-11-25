package com.cloud.arch.page;

import java.util.List;
import java.util.function.Function;

public class PageWrapper {

    private final PageCondition                    condition;
    private       Function<PageCondition, Integer> counter;

    public PageWrapper(PageCondition condition) {
        this.condition = condition;

    }

    public PageWrapper count(Function<PageCondition, Integer> counter) {
        this.counter = counter;
        return this;
    }

    public <T> Page<T> query(Function<PageCondition, List<T>> dataLoader) {
        Page<T> page = new Page<>();
        page.setCurrent(this.condition.getPage());
        page.setPageSize(this.condition.getLimit());
        if (this.counter == null) {
            List<T> records = dataLoader.apply(condition);
            page.setRecords(records);
            page.setSize(records.size());
            return page;
        }
        int total = this.counter.apply(condition);
        page.setTotal(total);
        if (total > 0 && condition.getOffset() <= total) {
            List<T> records = dataLoader.apply(condition);
            page.setRecords(records);
            page.setSize(records.size());
        }
        return page;
    }

    public <T> List<T> list(Function<PageCondition, List<T>> dataLoader) {
        return dataLoader.apply(this.condition);
    }

}
