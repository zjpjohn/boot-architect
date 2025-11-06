package com.cloud.arch.rocket.idempotent;

import java.util.Date;

public interface IdempotentChecker {

    /**
     * 判断消息是否已经处理
     *
     * @param key 校验标识
     * @param cls 幂等验证类型:1-根据消息id,2-根据业务标识(如果业务标识存在业务标识优先)
     */
    boolean isProcessed(String key, Integer cls);


    /**
     * 标记消息是否已经处理
     *
     * @param key 校验标识
     * @param cls 幂等校验类型
     * @param e   异常
     */
    void markProcessed(String key, Integer cls, Throwable e);

    /**
     * 回收处理
     *
     * @param before 回收时间
     */
    void garbageCollect(Date before);
}
