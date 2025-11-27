package com.cloud.arch.operate.application;

import com.cloud.arch.operate.application.dto.LogListQuery;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.page.Pager;

public interface ILogQueryService {

    OperationLog operationLog(Long id);

    Pager<OperationLog> logList(LogListQuery query);

}
