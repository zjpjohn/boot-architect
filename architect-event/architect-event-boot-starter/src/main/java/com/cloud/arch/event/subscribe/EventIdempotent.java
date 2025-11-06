package com.cloud.arch.event.subscribe;

import lombok.Data;

@Data
public class EventIdempotent {
    /**
     * 事件名称
     */
    private String name;
    /**
     * 事件条件tag
     */
    private String filter;
    /**
     * 事件幂等key
     */
    private String eventKey;
    /**
     * 幂等事件分片键
     */
    private String shardKey;

}
