package com.cloud.arch.operate.adapter;


import com.cloud.arch.operate.application.ILogQueryService;
import com.cloud.arch.operate.application.dto.LogListQuery;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.page.Page;
import com.cloud.arch.web.annotation.ApiBody;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/oper/log")
@RequiredArgsConstructor
public class OperLogController {

    private final ILogQueryService logQueryService;

    @GetMapping
    public OperationLog operationLog(@NotNull(message = "log id must not be null") Long id) {
        return logQueryService.operationLog(id);
    }

    @GetMapping("/list")
    public Page<OperationLog> logList(@Validated LogListQuery query) {
        return logQueryService.logList(query);
    }

}
