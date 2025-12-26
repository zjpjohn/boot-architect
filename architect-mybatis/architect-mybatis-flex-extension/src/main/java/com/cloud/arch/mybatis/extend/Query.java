package com.cloud.arch.mybatis.extend;

import com.cloud.arch.page.Pager;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.mybatis.Mappers;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.CPI;
import com.mybatisflex.core.query.MapperQueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryWrapperAdapter;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.LambdaGetter;
import org.apache.ibatis.cursor.Cursor;

import java.util.Arrays;
import java.util.List;

public class Query<T> extends QueryWrapperAdapter<Query<T>> implements MapperQueryChain<T> {

    private final BaseMapper<T> baseMapper;

    public Query(BaseMapper<T> baseMapper) {
        this.baseMapper = baseMapper;
    }

    @Override
    public BaseMapper<T> baseMapper() {
        return baseMapper;
    }

    @Override
    public QueryWrapper toQueryWrapper() {
        return this;
    }


    @Override
    public String toSQL() {
        TableInfo tableInfo = TableInfoFactory.ofMapperClass(baseMapper.getClass());
        CPI.setFromIfNecessary(this, tableInfo.getSchema(), tableInfo.getTableName());
        return super.toSQL();
    }

    public Query<T> orderDesc(LambdaGetter<T> column) {
        super.orderBy(column, false);
        return this;
    }

    public Query<T> orderDesc(String... columns) {
        for (String column : columns) {
            super.orderBy(column, false);
        }
        return this;
    }

    public Query<T> orderDesc(String column) {
        super.orderBy(column, false);
        return this;
    }

    public Query<T> orderAsc(LambdaGetter<T> column) {
        super.orderBy(column, true);
        return this;
    }

    public Query<T> orderAsc(String column) {
        super.orderBy(column, true);
        return this;
    }

    public Query<T> orderAsc(String... columns) {
        for (String column : columns) {
            super.orderBy(column, true);
        }
        return this;
    }

    /**
     * 自行填充查询条件
     * 查询条件实现 Condition接口，自行填充Query查询条件
     */
    public Query<T> where(Condition condition) {
        super.and(condition);
        return this;
    }

    public Cursor<T> cursor() {
        return this.baseMapper.selectCursorByQuery(this);
    }

    public List<T> list() {
        return this.baseMapper.selectListByQuery(this);
    }

    public <R> List<R> listAs(Class<R> type) {
        return this.baseMapper.selectListByQueryAs(this, type);
    }

    public <R> Cursor<R> cursorAs(Class<R> type) {
        return this.baseMapper.selectCursorByQueryAs(this, type);
    }

    public Pager<T> pager(Page<T> page) {
        return this.convert(this.page(page));
    }

    public <R> Pager<R> pagerAs(Page<R> page, Class<R> type) {
        return this.convert(this.pageAs(page, type));
    }

    public Page<T> page(Number pageNumber, Number pageSize) {
        return this.page(Page.of(pageNumber, pageSize));
    }

    public Pager<T> pager(Number pageNumber, Number pageSize) {
        Page<T> page = this.page(Page.of(pageNumber, pageSize));
        return this.convert(page);
    }

    public Page<T> page(Number pageNumber, Number pageSize, Number totalRow) {
        return this.page(Page.of(pageNumber, pageSize, totalRow));
    }

    public Pager<T> pager(Number pageNumber, Number pageSize, Number totalRow) {
        Page<T> page = this.page(Page.of(pageNumber, pageSize, totalRow));
        return this.convert(page);
    }

    public Page<T> page(PageWhere where) {
        return where.page(this.where(where));
    }

    /**
     * 转换为外部分页
     */
    private <R> Pager<R> convert(Page<R> page) {
        Pager<R> pager = new Pager<>();
        pager.setTotal(page.getTotalRow());
        pager.setPageSize(page.getPageSize());
        pager.setCurrent(page.getPageNumber());
        pager.setRecords(page.getRecords());
        return pager;
    }

    public Pager<T> pager(PageWhere where) {
        return where.pager(this.where(where));
    }

    public static <E> Query<E> of(Class<E> entityClass) {
        BaseMapper<E> baseMapper = Mappers.ofEntityClass(entityClass);
        return new Query<>(baseMapper);
    }

    public static <E> Query<E> of(BaseMapper<E> mapper) {
        return new Query<>(mapper);
    }

}
