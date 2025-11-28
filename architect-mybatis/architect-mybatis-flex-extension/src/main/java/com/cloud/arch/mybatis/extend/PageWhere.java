package com.cloud.arch.mybatis.extend;

import com.cloud.arch.page.Pager;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.function.Consumer;

@Data
public class PageWhere implements Consumer<QueryWrapper> {

    @Min(value = 1, message = "page index must greater than 0")
    private Integer page  = 1;
    @Range(min = 1, max = 1000, message = "page size must between 1 and 1000")
    private Integer limit = 10;

    @Override
    public void accept(QueryWrapper wrapper) {

    }

    public <T> Page<T> page(Query<T> query) {
        return query.page(this.page, this.limit);
    }

    public <T> Pager<T> pager(Query<T> query) {
        return query.pager(this.page, this.limit);
    }

}
