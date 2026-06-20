package com.cloud.arch.event.core.publish;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventPublisher {

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     * @return 异步发送结果
     */
    CompletableFuture<Void> publish(EventMessage message);

    /**
     * 批量发送，返回每条消息的独立结果（部分成功部分失败时逐条标记）。
     * 默认逐条调用 {@link #publish}，支持原生批的 MQ 实现可覆写为单次网络往返 + 逐条状态。
     */
    default List<CompletableFuture<Void>> publishBatch(List<EventMessage> messages) {
        return messages.stream().map(this::publish).toList();
    }

}
