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
import org.apache.ibatis.cursor.Cursor;

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

    public Cursor<T> cursor() {
        return this.baseMapper.selectCursorByQuery(this);
    }

    public <R> Cursor<R> cursorAs(Class<R> type) {
        return this.baseMapper.selectCursorByQueryAs(this, type);
    }

    public Pager<T> pager(Page<T> page) {
        return PagerConverter.convert(this.page(page));
    }

    public <R> Pager<R> pagerAs(Page<R> page, Class<R> type) {
        return PagerConverter.convert(this.pageAs(page, type));
    }

    public Page<T> page(Number pageNumber, Number pageSize) {
        return this.page(Page.of(pageNumber, pageSize));
    }

    public Pager<T> pager(Number pageNumber, Number pageSize) {
        Page<T> page = this.page(Page.of(pageNumber, pageSize));
        return PagerConverter.convert(page);
    }

    public Page<T> page(Number pageNumber, Number pageSize, Number totalRow) {
        return this.page(Page.of(pageNumber, pageSize, totalRow));
    }

    public Pager<T> pager(Number pageNumber, Number pageSize, Number totalRow) {
        Page<T> page = this.page(Page.of(pageNumber, pageSize, totalRow));
        return PagerConverter.convert(page);
    }

    public static <E> Query<E> of(Class<E> entityClass) {
        BaseMapper<E> baseMapper = Mappers.ofEntityClass(entityClass);
        return new Query<>(baseMapper);
    }

    public static <E> Query<E> of(BaseMapper<E> mapper) {
        return new Query<>(mapper);
    }

}
