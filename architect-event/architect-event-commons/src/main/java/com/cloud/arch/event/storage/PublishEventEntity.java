package com.cloud.arch.event.storage;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventState;
import com.cloud.arch.event.core.publish.Version;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PublishEventEntity implements Version {

    private Long       id;
    /**
     * 业务分组便于区分业务
     */
    private String     bizGroup;
    /**
     * 发送消息topic
     */
    private String     name;
    /**
     * 发送消息过滤tag
     */
    private String     filter;
    /**
     * 延迟时间
     */
    private Long       delay;
    /**
     * 消息内容
     */
    private String     event;
    /**
     * 消息分片key(分库分表时使用，保证业务表和事件表在同一库中)
     */
    private String     shardingKey;
    /**
     * 消息状态
     */
    private EventState state;
    /**
     * 消息版本
     */
    private Integer    version;
    /**
     * 创建时间
     */
    private Long       gmtCreate;
    /**
     * 发布成功时间
     */
    private Long       publishedTime;

    public PublishEventEntity(Long id) {
        this.id = id;
    }

    public EventMessage build() {
        EventMessage message = new EventMessage();
        message.setName(name);
        message.setFilter(filter);
        message.setDelay(delay);
        message.setData(event);
        message.setKey(String.valueOf(id));
        return message;
    }

    public Integer getEventState() {
        return state.getState();
    }
}
