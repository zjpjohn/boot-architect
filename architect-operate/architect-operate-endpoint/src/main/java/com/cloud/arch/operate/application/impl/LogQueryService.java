package com.cloud.arch.operate.application.impl;

import com.cloud.arch.operate.application.ILogQueryService;
import com.cloud.arch.operate.application.dto.LogListQuery;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.operate.infrast.error.OptErrorHandler;
import com.cloud.arch.operate.infrast.props.OperateProperties;
import com.cloud.arch.operate.infrast.repository.LogRepository;
import com.cloud.arch.page.Page;
import com.cloud.arch.web.error.ApiBizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(OperateProperties.class)
public class LogQueryService implements ILogQueryService {

    private final LogRepository     repository;
    private final OperateProperties properties;

    @Override
    public OperationLog operationLog(Long id) {
        OperationLog log = repository.query(id);
        OptErrorHandler.LOG_NOT_EXIST.check(log);
        return log;
    }

    @Override
    public Page<OperationLog> logList(LogListQuery query) {
        if (properties.getTenantForce() && StringUtils.isBlank(query.getTenantId())) {
            throw new ApiBizException(HttpStatus.BAD_REQUEST, 400, "tenantId must not be null.");
        }
        return repository.queryList(query.from());
    }

}
