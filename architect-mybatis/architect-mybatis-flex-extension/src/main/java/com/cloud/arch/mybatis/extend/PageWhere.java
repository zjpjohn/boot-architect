package com.cloud.arch.mybatis.extend;

import com.cloud.arch.page.PageQuery;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.function.Consumer;

public class PageWhere extends PageQuery implements Consumer<QueryWrapper> {

    @Override
    public void accept(QueryWrapper wrapper) {

    }

    public <T> Page<T> of() {
        return Page.of(this.getPage(), this.getLimit());
    }

}
