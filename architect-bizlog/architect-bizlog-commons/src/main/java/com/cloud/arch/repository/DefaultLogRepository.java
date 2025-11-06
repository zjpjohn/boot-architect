package com.cloud.arch.repository;


import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Page;
import com.cloud.arch.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class DefaultLogRepository implements ILogRepository {

    @Override
    public void save(List<LogRecord> records) {
        log.info("操作日志内容:{}", JsonUtils.toJson(records));
    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        return Collections.emptyList();
    }

    @Override
    public Page<LogRecord> queryPage(LogPageQuery query) {
        return Page.empty(query.getLimit());
    }

}
