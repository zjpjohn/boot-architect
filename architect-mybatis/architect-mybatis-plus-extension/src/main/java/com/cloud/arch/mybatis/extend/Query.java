package com.cloud.arch.mybatis.extend;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class Query<T> extends AbstractWrapper<T, String, Query<T>>
        implements com.baomidou.mybatisplus.core.conditions.query.Query<Query<T>, T, String> {

    protected final SharedString sqlSelect;
    private         boolean      checkSqlInjection;

    public Query() {
        this((T) null);
    }

    public Query(T entity) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.initNeed();
    }

    public Query(Class<T> entityClass) {
        this.sqlSelect = new SharedString();
        super.setEntityClass(entityClass);
        super.initNeed();
    }

    public Query(T entity, String... columns) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.initNeed();
        this.select(columns);
    }

    public Query(T entity,
                 Class<T> entityClass,
                 AtomicInteger paramNameSeq,
                 Map<String, Object> paramNameValuePairs,
                 MergeSegments mergeSegments,
                 SharedString paramAlias,
                 SharedString lastSql,
                 SharedString sqlComment,
                 SharedString sqlFirst) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq        = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression          = mergeSegments;
        this.paramAlias          = paramAlias;
        this.lastSql             = lastSql;
        this.sqlComment          = sqlComment;
        this.sqlFirst            = sqlFirst;
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
     * 条件更新
     */
    public int update(Function<Query<T>, Integer> loader) {
        return loader.apply(this);
    }

    /**
     * 条件删除
     */
    public int delete(Function<Query<T>, Integer> loader) {
        return loader.apply(this);
    }

    /**
     * 查询单条数据
     */
    public T one(Function<Query<T>, T> loader) {
        return loader.apply(this);
    }

    /**
     * 查询列表
     */
    public List<T> list(Function<Query<T>, List<T>> loader) {
        return loader.apply(this);
    }

    /**
     * 分页查询构造
     */
    public PageWrapper<T> page(Page<T> page) {
        return new PageWrapper<>(page, this);
    }


    public static <E> Query<E> from() {
        return new Query<>();
    }

    public static <E> Query<E> from(E entity) {
        return new Query<>(entity);
    }

    public static <E> Query<E> from(Class<E> clazz) {
        return new Query<>(clazz);
    }

    public static <E> Query<E> from(E entity, String... columns) {
        return new Query<>(entity, columns);
    }

}
