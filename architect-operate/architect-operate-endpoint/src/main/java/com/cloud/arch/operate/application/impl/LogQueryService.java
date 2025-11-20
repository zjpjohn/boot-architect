package com.cloud.arch.operate.application.impl;

import com.cloud.arch.operate.application.ILogQueryService;
import com.cloud.arch.operate.application.dto.LogListQuery;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.operate.infrast.error.OptErrorHandler;
import com.cloud.arch.operate.infrast.repository.LogRepository;
import com.cloud.arch.page.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService implements ILogQueryService {

    private final LogRepository repository;

    @Override
    public OperationLog operationLog(Long id) {
        OperationLog log = repository.query(id);
        OptErrorHandler.LOG_NOT_EXIST.check(log);
        return log;
    }

    @Override
    public Page<OperationLog> logList(LogListQuery query) {
        return repository.queryList(query.from());
    }

}
