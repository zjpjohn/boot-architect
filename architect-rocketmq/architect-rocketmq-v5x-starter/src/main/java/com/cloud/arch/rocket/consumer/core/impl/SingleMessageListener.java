package com.cloud.arch.rocket.consumer.core.impl;

import com.cloud.arch.rocket.consumer.core.ListenerMetadata;
import com.cloud.arch.rocket.consumer.core.MessageListener;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import com.google.common.collect.HashBasedTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.rocketmq.common.message.MessageExt;

@Slf4j
public class SingleMessageListener implements MessageListener {

    private final HashBasedTable<String, String, ListenerMetadata> metadataMap;
    private final Serialize                                        serialize;

    public SingleMessageListener(HashBasedTable<String, String, ListenerMetadata> metadataMap, Serialize serialize) {
        this.metadataMap = metadataMap;
        this.serialize   = serialize;
    }

    /**
     * 单条消息处理
     *
     * @param message 消息内容
     */
    @Override
    public void handle(MessageExt message) throws Exception {
        ListenerMetadata metadata = metadataMap.get(message.getTopic(), message.getTags());
        if (metadata == null) {
            String error = String.format("未找到对应消息topic[%s]-tag[{%s}]的消息监听器,",
                                         message.getTopic(),
                                         message.getTags());
            throw new RuntimeException(error);
        }
        Throwable             error             = null;
        Pair<String, Integer> idempotent        = null;
        IdempotentChecker     idempotentChecker = metadata.getIdempotentChecker();
        try {
            if (metadata.idempotent() && idempotentChecker != null) {
                idempotent = metadata.extractIdempotent(message);
                if (idempotentChecker.isProcessed(idempotent.getKey(), idempotent.getValue())) {
                    return;
                }
            }
            metadata.invoke(message, serialize);
        } catch (Exception exception) {
            log.error("消息监听消费处理错误:", exception);
            throw new RuntimeException(exception);
        } finally {
            if (metadata.idempotent() && idempotentChecker != null && idempotent != null) {
                idempotentChecker.markProcessed(idempotent.getKey(), idempotent.getValue(), error);
            }
        }
    }

    public Serialize getSerialize() {
        return serialize;
    }

}
