package com.cloud.arch.repository;


import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Pager;

import java.util.List;

public interface ILogQueryService {

    List<LogRecord> ofBizNo(String bizNo);

    Pager<LogRecord> queryList(LogPageQuery query);

}
