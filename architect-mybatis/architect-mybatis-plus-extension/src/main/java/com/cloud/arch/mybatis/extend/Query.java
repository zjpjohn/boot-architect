package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.arch.page.Pager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Query<T> extends AbstractWrapper<T, String, Query<T>> implements com.baomidou.mybatisplus.core.conditions.query.Query<Query<T>, T, String> {

    protected final SharedString  sqlSelect;
    private         boolean       checkSqlInjection;
    private         BaseMapper<T> mapper;

    public Query(BaseMapper<T> mapper) {
        this((T) null, mapper);
    }

    public Query(T entity, BaseMapper<T> mapper) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.initNeed();
        this.mapper = mapper;
    }

    public Query(Class<T> entityClass, BaseMapper<T> mapper) {
        this.sqlSelect = new SharedString();
        super.setEntityClass(entityClass);
        super.initNeed();
        this.mapper = mapper;
    }

    public Query(T entity, BaseMapper<T> mapper, String... columns) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.initNeed();
        this.select(columns);
        this.mapper = mapper;
    }

    public Query(T entity, Class<T> entityClass, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs,
                 MergeSegments mergeSegments, SharedString paramAlias, SharedString lastSql, SharedString sqlComment,
                 SharedString sqlFirst) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    @Override
    protected Query<T> instance() {
        return new Query<T>(this.getEntity(),
                            this.getEntityClass(),
                            this.paramNameSeq,
                            this.paramNameValuePairs,
                            new MergeSegments(),
                            this.paramAlias,
                            SharedString.emptyString(),
                            SharedString.emptyString(),
                            SharedString.emptyString());
    }

    /**
     * 开启检查 SQL 注入
     */
    public Query<T> checkSqlInjection() {
        this.checkSqlInjection = true;
        return this;
    }

    @Override
    protected String columnToString(String column) {
        if (checkSqlInjection && SqlInjectionUtils.check(column)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + column);
        }
        return column;
    }

    @Override
    public Query<T> select(boolean condition, List<String> columns) {
        if (condition && CollectionUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(String.join(StringPool.COMMA, columns));
        }
        return typedThis;
    }

    @Override
    public Query<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        super.setEntityClass(entityClass);
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(getEntityClass()).chooseSelect(predicate));
        return typedThis;
    }

    /**
     * 转换为lambada query 支持lambda写法
     */
    public LambdaQuery<T> lambda() {
        return new LambdaQuery<>(getEntity(),
                                 getEntityClass(),
                                 sqlSelect,
                                 paramNameSeq,
                                 paramNameValuePairs,
                                 expression,
                                 paramAlias,
                                 lastSql,
                                 sqlComment,
                                 sqlFirst);
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
    }

    /**
     * 自行填充查询条件
     * 查询条件实现Consumer接口
     */
    public Query<T> condition(Consumer<Query<T>> consumer) {
        consumer.accept(this);
        return this;
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
        return Optional.ofNullable(one());
    }

    /**
     * 查询列表
     */
    public List<T> list() {
        return mapper.selectList(this);
    }

    public Page<T> page(Integer current, Integer size, boolean searchCount) {
        Page<T> page = Page.of(current, size, searchCount);
        return this.mapper.selectPage(page, this);
    }

    public Pager<T> pager(Integer current, Integer size, boolean searchCount) {
        return PagerConverter.convert(this.page(current, size, searchCount));
    }

    /**
     * 分页查询
     */
    public Page<T> page(Integer current, Integer size) {
        Page<T> page = Page.of(current, size);
        return mapper.selectPage(page, this);
    }


    public Pager<T> pager(Integer current, Integer size) {
        return PagerConverter.convert(this.page(current, size));
    }

    public Page<T> page(Page<T> page) {
        return mapper.selectPage(page, this);
    }

    public Pager<T> pager(Page<T> page) {
        return PagerConverter.convert(this.page(page));
    }

    public static <E> Query<E> of(BaseMapper<E> mapper) {
        return new Query<>(mapper);
    }

    public static <E> Query<E> of(E entity, BaseMapper<E> mapper) {
        return new Query<>(entity, mapper);
    }

    public static <E> Query<E> of(Class<E> clazz, BaseMapper<E> mapper) {
        return new Query<>(clazz, mapper);
    }

    public static <E> Query<E> of(E entity, BaseMapper<E> mapper, String... columns) {
        return new Query<>(entity, mapper, columns);
    }

}
