package com.cloud.arch.event.core.publish;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventMessage {
    /**
     * 消息路由名称
     */
    private String name;
    /**
     * 消息路由过滤
     */
    private String filter;
    /**
     * 消息延迟时间
     */
    private Long   delay;
    /**
     * 消息全局唯一标识
     */
    private String key;
    /**
     * 消息内容
     */
    private String data;

}
