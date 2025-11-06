package com.cloud.arch.operate.application;

import com.cloud.arch.operate.application.dto.LogListQuery;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.page.Page;

public interface ILogQueryService {

    OperationLog operationLog(Long id);

    Page<OperationLog> logList(LogListQuery query);

}
