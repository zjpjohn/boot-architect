package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.utils.IdWorker;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class GenericEventConverter {

    /**
     * 泛化领域事件转换为事件实体
     *
     * @param event 泛化事件
     * @param codec 事件编码器
     */
    public static PublishEventEntity toEntity(GenericEvent event, EventCodec codec) {
        Assert.notNull(codec, "事件序列化器不允许为空");
        Assert.notNull(event.event(), "远程事件内容不允许为空");
        Assert.state(StringUtils.hasText(event.name()), "远程事件消息主题不允许为空.");
        PublishEventEntity entity = new PublishEventEntity();
        entity.setId(IdWorker.nextId());
        entity.setBizGroup(event.bizGroup());
        entity.setName(event.name());
        entity.setFilter(event.filter());
        entity.setDelay(event.delay());
        entity.setState(EventState.INITIALIZED);
        entity.setShardingKey(event.shardingKey());
        entity.setVersion(Version.INITIAL_VERSION);
        entity.setGmtCreate(System.currentTimeMillis());
        entity.setEvent(codec.encode(event.event()));
        return entity;
    }

}
