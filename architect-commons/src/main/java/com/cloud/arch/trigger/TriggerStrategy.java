package com.cloud.arch.trigger;

public interface TriggerStrategy {

    /**
     * 唤醒任务执行
     */
    void wakeUp();

    /**
     * 关闭任务
     */
    void shutdown();

}
