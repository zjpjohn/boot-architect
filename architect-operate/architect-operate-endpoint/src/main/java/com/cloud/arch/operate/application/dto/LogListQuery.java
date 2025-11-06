package com.cloud.arch.operate.application.dto;

import com.cloud.arch.operate.core.OperateType;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.web.validation.annotation.Enumerable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogListQuery extends PageQuery {

    private String    appNo;
    private String    bizGroup;
    private Integer   state;
    @Enumerable(enums = OperateType.class, message = "操作类型错误")
    private String    type;
    private Long      opId;
    private LocalDate start;
    private LocalDate end;

}
