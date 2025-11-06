package com.cloud.arch.page;

import com.cloud.arch.utils.CollectionUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Page<T> implements Serializable {

    private int total = 0;

    private int current = 1;

    private int pageSize = 10;

    private int size = 0;

    private List<T> records = Collections.emptyList();

    /**
     * 分页结果数据构造
     *
     * @param condition     分页查询条件
     * @param countFunction total操作函数 {@link Function}
     * @param dataFunction  data查询操作 {@link Function}
     * @return 返回分页数据
     */
    public static <E> Page<E> wrapper(PageCondition condition,
                                      Function<PageCondition, Integer> countFunction,
                                      Function<PageCondition, List<E>> dataFunction) {
        return PageWrapper.wrap(condition, countFunction).query(dataFunction);
    }

    /**
     * 空分页信息
     *
     * @param limit 分页数据
     */
    public static <E> Page<E> empty(Integer limit) {
        Page<E> result = new Page<>();
        result.setPageSize(limit);
        return result;
    }

    /**
     * page 分页数据转换
     *
     * @param function 数据转换
     * @param <V>      转换后类型
     */
    public <V> Page<V> map(Function<T, V> function) {
        Page<V> vPage = new Page<>();
        vPage.setTotal(this.total);
        vPage.setPageSize(this.pageSize);
        vPage.setCurrent(this.current);
        if (this.total > 0) {
            vPage.setSize(this.size);
            List<V> list = this.records.stream().map(function).collect(Collectors.toList());
            vPage.setRecords(list);
        }
        return vPage;
    }

    /**
     * page分页数据转换
     *
     * @param function 数据转换器
     * @param <V>      转换后数据
     */
    public <V> Page<V> flatMap(Function<List<T>, List<V>> function) {
        Page<V> vPage = new Page<>();
        vPage.setTotal(this.total);
        vPage.setPageSize(this.pageSize);
        vPage.setCurrent(this.current);
        if (this.total > 0) {
            vPage.setSize(this.size);
            vPage.setRecords(function.apply(this.records));
        }
        return vPage;
    }

    /**
     * 分页数据处理
     *
     * @param consumer 消费处理器
     */
    public Page<T> ifPresent(Consumer<List<T>> consumer) {
        if (CollectionUtils.isNotEmpty(this.records)) {
            consumer.accept(this.records);
        }
        return this;
    }

    /**
     * 前置条件判断处理
     *
     * @param predicate 前置条件
     * @param consumer  数据处理
     */
    public Page<T> ifPresent(Predicate<List<T>> predicate, Consumer<List<T>> consumer) {
        if (predicate.test(this.records) && CollectionUtils.isNotEmpty(this.records)) {
            consumer.accept(this.records);
        }
        return this;
    }

    /**
     * 处理每一个元素
     *
     * @param consumer 消费处理
     */
    public Page<T> forEach(Consumer<T> consumer) {
        if (CollectionUtils.isNotEmpty(this.records)) {
            this.records.forEach(consumer);
        }
        return this;
    }

}
