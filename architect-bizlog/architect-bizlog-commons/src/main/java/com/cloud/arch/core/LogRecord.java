package com.cloud.arch.core;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LogRecord implements Serializable {

    private static final long serialVersionUID = -8706483779986510727L;

    private String        id;
    /**
     * 所属应用
     */
    private String        app;
    /**
     * 业务分组
     */
    private String        group      = "";
    /**
     * 多租户标识
     */
    private String        tenant     = "";
    /**
     * 业务操作编号
     */
    private String        bizNo      = "";
    /**
     * 操作员标识
     */
    private String        operatorId = "";
    /**
     * 操作人员
     */
    private String        operator   = "";
    /**
     * 操作动作内容
     */
    private String        action     = "";
    /**
     * 操作日志状态：0-操作成功,1-操作失败
     */
    private Integer       fail       = 0;
    /**
     * 操作对象详情
     */
    private String        detail     = "";
    /**
     * 操作时间
     */
    private LocalDateTime gmtCreate;

}
