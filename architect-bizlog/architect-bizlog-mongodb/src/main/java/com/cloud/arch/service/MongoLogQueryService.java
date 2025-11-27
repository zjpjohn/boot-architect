package com.cloud.arch.service;


import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Pager;
import com.cloud.arch.repository.ILogQueryService;
import com.cloud.arch.repository.ILogRepository;

import java.util.List;

public class MongoLogQueryService implements ILogQueryService {

    private final ILogRepository logRepository;

    public MongoLogQueryService(ILogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        return logRepository.ofBizNo(bizNo);
    }

    @Override
    public Pager<LogRecord> queryList(LogPageQuery query) {
        return logRepository.queryPage(query);
    }
}
