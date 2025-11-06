package com.cloud.arch.event.storage;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventCompensateEntity {

    private Long          id;
    /**
     * 事件标识
     */
    private Long          eventId;
    /**
     * 事件分片键（分库分表使用，保证补偿信息和事件信息在同一库中）
     */
    private String        shardingKey;
    /**
     * 补偿开始时间
     */
    private Long          startTime;
    /**
     * 补偿耗时
     */
    private Long          taken;
    /**
     * 补偿错误日志消息
     */
    private String        failedMsg;
    /**
     * 记录创建时间
     */
    private LocalDateTime gmtCreate;

}
