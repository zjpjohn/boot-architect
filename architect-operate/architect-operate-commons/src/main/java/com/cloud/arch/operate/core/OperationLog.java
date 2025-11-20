package com.cloud.arch.operate.core;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {

    /**
     * 操作日志流水号
     */
    private Long          id;
    /**
     * 应用编号
     */
    private String        appNo;
    /**
     * 租户标识
     */
    private String        tenantId;
    /**
     * 操作业务分组
     */
    private String        bizGroup;
    /**
     * 操作说明标题
     */
    private String        title;
    /**
     * 操作类型
     */
    private OperateType   type;
    /**
     * 操作目标类方法
     */
    private String        target;
    /**
     * 操作请求方法
     */
    private String        method;
    /**
     * 操作请求uri
     */
    private String        reqUri;
    /**
     * 操作人id
     */
    private Long          opId;
    /**
     * 操作人名称
     */
    private String        opName;
    /**
     * 操作人ip
     */
    private String        opIp;
    /**
     * 操作地址
     */
    private String        opLocation;
    /**
     * 操作结果状态:0-失败,1-成功
     */
    private Integer       state;
    /**
     * 操作请求入参
     */
    private String        params;
    /**
     * 操作失败信息
     */
    private String        error;
    /**
     * 方法执行耗时时间-毫秒
     */
    private Long          takenTime;
    /**
     * 操作时间
     */
    private LocalDateTime gmtCreate;

}
