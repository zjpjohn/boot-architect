package com.cloud.arch.support.core;

import com.cloud.arch.core.LogRecord;
import com.cloud.arch.repository.ILogRepository;
import com.cloud.arch.utils.IdWorker;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyLogRepository {

    private final ILogRepository     logRepository;
    private final AsyncLogDispatcher asyncLogDispatcher;

    public ProxyLogRepository(ILogRepository logRepository, AsyncLogDispatcher asyncLogDispatcher) {
        this.logRepository      = logRepository;
        this.asyncLogDispatcher = asyncLogDispatcher;
    }

    public void saveRecord(LogRecord logRecord) {
        logRecord.setId(String.valueOf(IdWorker.nextId()));
        if (asyncLogDispatcher != null) {
            this.asyncLogDispatcher.publishRecord(logRecord);
            return;
        }
        logRepository.save(Lists.newArrayList(logRecord));
    }

    public ILogRepository getLogRepository() {
        return logRepository;
    }

    public AsyncLogDispatcher getAsyncLogDispatcher() {
        return asyncLogDispatcher;
    }
}
