package com.cloud.arch.service;


import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Page;
import com.cloud.arch.repository.ILogQueryService;
import com.cloud.arch.repository.ILogRepository;

import java.util.List;

public class ElasticLogQueryService implements ILogQueryService {

    private final ILogRepository logRepository;

    public ElasticLogQueryService(ILogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        return logRepository.ofBizNo(bizNo);
    }

    @Override
    public Page<LogRecord> queryList(LogPageQuery query) {
        return logRepository.queryPage(query);
    }

}
