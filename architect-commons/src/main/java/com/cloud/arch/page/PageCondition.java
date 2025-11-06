package com.cloud.arch.page;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PageCondition implements Serializable {

    private static final long serialVersionUID = -2061071494603748487L;

    private int limit  = 10;
    private int offset = 0;
    private int page   = 1;

    private final Map<String, Object> condition = Maps.newHashMap();

    public static PageCondition build() {
        return new PageCondition();
    }

    public static PageCondition build(int limit) {
        return new PageCondition().setLimit(limit);
    }

    public int getLimit() {
        return limit;
    }

    public PageCondition setLimit(int limit) {
        if (limit > 0) {
            this.limit  = limit;
            this.offset = (this.page - 1) * limit;
        }
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public PageCondition setOffset(int offset) {
        if (offset >= 0) {
            this.offset = offset;
        }
        return this;
    }

    public int getPage() {
        return page;
    }

    public PageCondition setPage(int page) {
        if (page >= 1) {
            this.page   = page;
            this.offset = (page - 1) * limit;
        }
        return this;
    }

    public Map<String, Object> getCondition() {
        return Collections.unmodifiableMap(condition);
    }

    public PageCondition setParam(String key, Object value) {
        condition.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        return (T) condition.get(key);
    }

    public PageWrapper count(Function<PageCondition, Integer> counter) {
        return PageWrapper.wrap(this, counter);
    }

    public <T> List<T> list(Function<PageCondition, List<T>> query) {
        return query.apply(this);
    }
}
