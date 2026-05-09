package com.cloud.arch.event.subscribe;

import lombok.Data;

/**
 * 幂等事件标识，包含事件名称、过滤标签、幂等键和分片键，用于幂等检查和路由。
 */
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
