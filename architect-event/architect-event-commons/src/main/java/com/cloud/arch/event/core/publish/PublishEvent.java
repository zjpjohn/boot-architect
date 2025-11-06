package com.cloud.arch.event.core.publish;


import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.IdWorker;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Getter
public class PublishEvent {

    private final String          shardingKey;
    private final PublishMetadata metadata;
    private final Object          event;

    public PublishEvent(String shardingKey, PublishMetadata metadata, Object event) {
        this.metadata    = metadata;
        this.event       = event;
        this.shardingKey = shardingKey;
    }

    /**
     * 创建本地事件
     *
     * @param event 事件内容
     */
    public static PublishEvent localEvent(Object event, PublishMetadata metadata) {
        return new PublishEvent(null, metadata, event);
    }

    /**
     * 创建跨应用事件
     *
     * @param shardingKey 事件分片键
     * @param metadata    时间元数据
     * @param event       事件内容
     */
    public static PublishEvent remoteEvent(String shardingKey, PublishMetadata metadata, Object event) {
        Assert.state(StringUtils.hasText(metadata.getName()), "事件名称不允许为空.");
        return new PublishEvent(shardingKey, metadata, event);
    }

    /**
     * 转换成领域事件实体
     */
    public PublishEventEntity toEntity() {
        Assert.state(!metadata.isLocal(), "发布事件必须为远程消息事件");
        PublishEventEntity entity = new PublishEventEntity();
        entity.setId(IdWorker.nextId());
        entity.setBizGroup(metadata.getBizGroup());
        entity.setName(metadata.getName());
        entity.setFilter(metadata.getFilter());
        entity.setDelay(metadata.getDelay());
        entity.setGmtCreate(System.currentTimeMillis());
        entity.setState(EventState.INITIALIZED);
        entity.setVersion(Version.INITIAL_VERSION);
        entity.setShardingKey(shardingKey);
        entity.setEvent((String) event);
        return entity;
    }

    /**
     * 转换成领域事件消息
     */
    public EventMessage toMessage() {
        Assert.state(!metadata.isLocal(), "发布事件必须为远程消息事件");
        EventMessage message = new EventMessage();
        message.setKey(String.valueOf(IdWorker.nextId()));
        message.setData((String) event);
        message.setName(metadata.getName());
        message.setFilter(metadata.getFilter());
        message.setDelay(metadata.getDelay());
        return message;
    }

}
