package com.cloud.arch.event.subscribe;

import java.time.LocalDateTime;

public interface IdempotentChecker {

    /**
     * 判断消息是否已经处理
     *
     * @param idempotent 幂等信息
     */
    boolean isProcessed(EventIdempotent idempotent);

    /**
     * 标记消息是否已经处理
     *
     * @param idempotent 幂等信息
     * @param e 异常
     */
    void markProcessed(EventIdempotent idempotent, Throwable e);

    /**
     * 回收幂等记录
     *
     * @param before 回收时间
     */
    void garbageClean(LocalDateTime before);
}
