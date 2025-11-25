package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class LambdaQuery<T> extends AbstractLambdaWrapper<T, LambdaQuery<T>>
        implements Query<LambdaQuery<T>, T, SFunction<T, ?>> {

    private SharedString sqlSelect = new SharedString();

    public LambdaQuery() {
        this((T) null);
    }

    public LambdaQuery(T entity) {
        super.setEntity(entity);
        super.initNeed();
    }

    public LambdaQuery(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
    }

    public LambdaQuery(T entity,
                       Class<T> entityClass,
                       SharedString sqlSelect,
                       AtomicInteger paramNameSeq,
                       Map<String, Object> paramNameValuePairs,
                       MergeSegments mergeSegments,
                       SharedString paramAlias,
                       SharedString lastSql,
                       SharedString sqlComment,
                       SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq        = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression          = mergeSegments;
        this.sqlSelect           = sqlSelect;
        this.paramAlias          = paramAlias;
        this.lastSql             = lastSql;
        this.sqlComment          = sqlComment;
        this.sqlFirst            = sqlFirst;
    }


    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>故 sqlSelect 不向下传递</p>
     */
    @Override
    protected LambdaQuery<T> instance() {
        return new LambdaQuery<>(getEntity(),
                                 getEntityClass(),
                                 null,
                                 paramNameSeq,
                                 paramNameValuePairs,
                                 new MergeSegments(),
                                 paramAlias,
                                 SharedString.emptyString(),
                                 SharedString.emptyString(),
                                 SharedString.emptyString());
    }


    @Override
    public LambdaQuery<T> select(SFunction<T, ?>... columns) {
        return doSelect(true, CollectionUtils.toList(columns));
    }

    @Override
    public LambdaQuery<T> select(boolean condition, SFunction<T, ?>... columns) {
        return doSelect(condition, CollectionUtils.toList(columns));
    }

    @Override
    public LambdaQuery<T> select(boolean condition, List<SFunction<T, ?>> columns) {
        return doSelect(condition, columns);
    }

    @Override
    public LambdaQuery<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        if (entityClass == null) {
            entityClass = getEntityClass();
        } else {
            setEntityClass(entityClass);
        }
        Assert.notNull(entityClass, "entityClass can not be null");
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(entityClass).chooseSelect(predicate));
        return typedThis;
    }

    protected LambdaQuery<T> doSelect(boolean condition, List<SFunction<T, ?>> columns) {
        if (condition && CollectionUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(columnsToString(false, columns));
        }
        return typedThis;
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
    }

    /**
     * 条件更新
     */
    public int update(Function<LambdaQuery<T>, Integer> loader) {
        return loader.apply(this);
    }

    /**
     * 条件删除
     */
    public int delete(Function<LambdaQuery<T>, Integer> loader) {
        return loader.apply(this);
    }

    /**
     * 查询单条数据
     */
    public T one(Function<LambdaQuery<T>, T> loader) {
        return loader.apply(this);
    }

    /**
     * 查询单条数据
     *
     */
    public Optional<T> oneNullable(Function<LambdaQuery<T>, T> loader) {
        return Optional.ofNullable(loader.apply(this));
    }

    /**
     * 查询列表
     */
    public List<T> list(Function<LambdaQuery<T>, List<T>> loader) {
        return loader.apply(this);
    }

    /**
     * 分页查询
     */
    public PagerWrapper<T> page(Integer current, Integer size) {
        Pager<T> page = Pager.of(current, size);
        return new PagerWrapper<>(page, this);
    }

    /**
     * 分页查询构造
     */
    public PagerWrapper<T> page(Pager<T> page) {
        return new PagerWrapper<>(page, this);
    }

    public static <E> LambdaQuery<E> from() {
        return new LambdaQuery<>();
    }

    public static <E> LambdaQuery<E> from(E entity) {
        return new LambdaQuery<>(entity);
    }

    public static <E> LambdaQuery<E> from(Class<E> clazz) {
        return new LambdaQuery<>(clazz);
    }

}
