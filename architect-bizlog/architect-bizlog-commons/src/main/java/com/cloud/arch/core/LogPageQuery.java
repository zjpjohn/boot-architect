package com.cloud.arch.core;

import com.cloud.arch.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogPageQuery extends PageQuery {
    private static final long serialVersionUID = 4054696743696753325L;

    private String app;
    private String group;
    private String tenant;
    private String bizNo;
    private String operatorId;

}
