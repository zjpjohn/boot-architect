package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.arch.page.Pager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class LambdaQuery<T> extends AbstractLambdaWrapper<T, LambdaQuery<T>>
        implements Query<LambdaQuery<T>, T, SFunction<T, ?>> {

    private SharedString  sqlSelect = new SharedString();
    private BaseMapper<T> mapper;

    public LambdaQuery(BaseMapper<T> mapper) {
        this((T) null, mapper);
    }

    public LambdaQuery(T entity, BaseMapper<T> mapper) {
        super.setEntity(entity);
        super.initNeed();
        this.mapper = mapper;
    }

    public LambdaQuery(Class<T> entityClass, BaseMapper<T> mapper) {
        super.setEntityClass(entityClass);
        super.initNeed();
        this.mapper = mapper;
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
    public int update() {
        return mapper.update(this);
    }

    /**
     * 条件删除
     */
    public int delete() {
        return mapper.delete(this);
    }

    /**
     * 查询单条数据
     */
    public T one() {
        return mapper.selectOne(this);
    }

    /**
     * 查询单条数据
     *
     */
    public Optional<T> oneNullable() {
        return Optional.ofNullable(this.one());
    }

    /**
     * 查询列表
     */
    public List<T> list() {
        return this.mapper.selectList(this);
    }

    /**
     * 分页查询
     */
    public Page<T> page(Integer current, Integer size) {
        Page<T> page = Page.of(current, size);
        return this.mapper.selectPage(page, this);
    }

    public Pager<T> pager(Integer current, Integer size) {
        return PagerConverter.convert(this.page(current, size));
    }

    /**
     * 分页查询
     */
    public Page<T> page(Integer current, Integer size, boolean searchCount) {
        Page<T> page = Page.of(current, size, searchCount);
        return this.mapper.selectPage(page, this);
    }

    public Pager<T> pager(Integer current, Integer size, boolean searchCount) {
        return PagerConverter.convert(this.page(current, size, searchCount));
    }

    /**
     * 分页查询构造
     */
    public Page<T> page(Page<T> page) {
        return this.mapper.selectPage(page, this);
    }

    public Pager<T> pager(Page<T> page) {
        return PagerConverter.convert(this.page(page));
    }

    public static <E> LambdaQuery<E> of(BaseMapper<E> mapper) {
        return new LambdaQuery<>(mapper);
    }

    public static <E> LambdaQuery<E> of(E entity, BaseMapper<E> mapper) {
        return new LambdaQuery<>(entity, mapper);
    }

    public static <E> LambdaQuery<E> of(Class<E> clazz, BaseMapper<E> mapper) {
        return new LambdaQuery<>(clazz, mapper);
    }

}
