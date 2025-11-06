package com.cloud.arch.rocket.consumer.core;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class SingleMessageListener implements MessageListener {

    private final Map<String, ListenerMetadata> metadataMap;
    @Getter
    private final Serialize                     serialize;

    public SingleMessageListener(Map<String, ListenerMetadata> metadataMap, Serialize serialize) {
        this.metadataMap = metadataMap;
        this.serialize   = serialize;
    }

    @Override
    public Action consume(Message message, ConsumeContext context) {
        ListenerMetadata metadata = metadataMap.get(message.getTag());
        if (metadata == null) {
            return Action.ReconsumeLater;
        }
        Throwable             error             = null;
        Pair<String, Integer> idempotent        = null;
        IdempotentChecker     idempotentChecker = metadata.getIdempotentChecker();
        try {
            if (metadata.idempotent() && idempotentChecker != null) {
                idempotent = extractIdempotent(message);
                if (idempotentChecker.isProcessed(idempotent.getKey(), idempotent.getValue())) {
                    return Action.CommitMessage;
                }
            }
            metadata.invoke(message, serialize);
            return Action.CommitMessage;
        } catch (Exception e) {
            error = e;
            log.error("消费者消息处理错误:", error);
            return Action.ReconsumeLater;
        } finally {
            if (metadata.idempotent() && idempotentChecker != null && idempotent != null) {
                idempotentChecker.markProcessed(idempotent.getKey(), idempotent.getValue(), error);
            }
        }
    }

    /**
     * 抽取消息幂等标识
     *
     * @param message 消息队列原始消息
     */
    private Pair<String, Integer> extractIdempotent(Message message) {
        return Optional.ofNullable(message.getKey())
                       .filter(StringUtils::isNotBlank)
                       .map(v -> Pair.of(v, 1))
                       .orElseGet(() -> Pair.of(message.getMsgID(), 0));
    }

}
