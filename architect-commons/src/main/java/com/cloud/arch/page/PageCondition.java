package com.cloud.arch.page;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class PageCondition implements Serializable {

    private final PagerWrapper        pagerWrapper;
    private final Map<String, Object> condition = Maps.newHashMap();

    private int limit  = 10;
    private int offset = 0;
    private int page   = 1;

    public PageCondition() {
        this.pagerWrapper = new PagerWrapper(this);
    }

    public static PageCondition build() {
        return new PageCondition();
    }

    public static PageCondition build(int limit) {
        return new PageCondition().setLimit(limit);
    }

    public PageCondition setLimit(int limit) {
        if (limit > 0) {
            this.limit  = limit;
            this.offset = (this.page - 1) * limit;
        }
        return this;
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

    public PageCondition count(Function<PageCondition, Number> counter) {
        this.pagerWrapper.counter(counter);
        return this;
    }

    public <T> Pager<T> query(Function<PageCondition, List<T>> loader) {
        return this.pagerWrapper.query(loader);
    }

    /**
     * 分页仅返回数据列表
     */
    public <T> List<T> list(Function<PageCondition, List<T>> query) {
        return query.apply(this);
    }

}
