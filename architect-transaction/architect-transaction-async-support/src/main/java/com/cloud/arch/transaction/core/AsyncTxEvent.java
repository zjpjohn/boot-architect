package com.cloud.arch.transaction.core;

import com.cloud.arch.transaction.utils.AsyncTxState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
public class AsyncTxEvent {

    /**
     * 异步任务标识
     */
    private Long          id;
    /**
     * 异步任务key-className.methodName(.key:如果不为空，解决同一类下相同方法名)
     */
    private String        asyncKey;
    /**
     * 任务分片key
     */
    private String        shardKey;
    /**
     * 异步任务上下文参数
     */
    private AsyncTxParams data;
    /**
     * 事件版本
     */
    private String        version;
    /**
     * 异步任务状态
     */
    private Integer       state;
    /**
     * 最大重试次数
     */
    private Integer       maxRetry;
    /**
     * 重试时间基数
     */
    private Long          retryInterval;
    /**
     * 当前重试次数
     */
    private Integer       retries;
    /**
     * 下一次重试时间
     */
    private LocalDateTime nextTime;
    /**
     * 任务创建时间
     */
    private LocalDateTime gmtCreate;
    /**
     * 最新修改时间
     */
    private LocalDateTime gmtModify;

    public AsyncTxVersion getEventVersion() {
        return new AsyncTxVersion(this.version);
    }

    /**
     * 递增重试次数
     */
    public void incrementRetry() {
        this.retries = this.retries + 1;
    }

    /**
     * 是否为死信任务
     */
    public boolean isDead() {
        return state == AsyncTxState.DEAD || (this.retries >= this.maxRetry && state == AsyncTxState.FAIL);
    }

    /**
     * 计算下一次重试时间
     */
    public void calcNextTime() {
        long seconds = retryInterval << this.retries;
        this.nextTime = LocalDateTime.now().plusSeconds(seconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AsyncTxEvent txEvent = (AsyncTxEvent) o;
        return Objects.equals(id, txEvent.id) && Objects.equals(nextTime, txEvent.nextTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nextTime);
    }

}
