package com.cloud.arch.page;

import java.util.List;
import java.util.function.Function;

public class PagerWrapper {

    private final PageCondition                   condition;
    private       Function<PageCondition, Number> counter;

    public PagerWrapper(PageCondition condition) {
        this.condition = condition;
    }

    public void counter(Function<PageCondition, Number> counter) {
        this.counter = counter;
    }

    public <T> Pager<T> query(Function<PageCondition, List<T>> dataLoader) {
        Pager<T> page = new Pager<>();
        page.setCurrent(this.condition.getPage());
        page.setPageSize(this.condition.getLimit());
        if (this.counter == null) {
            List<T> records = dataLoader.apply(condition);
            page.setRecords(records);
            page.setSize(records.size());
            return page;
        }
        long total = this.counter.apply(condition).longValue();
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
