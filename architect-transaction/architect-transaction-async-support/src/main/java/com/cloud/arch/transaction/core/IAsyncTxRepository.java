package com.cloud.arch.transaction.core;

import java.time.Duration;
import java.util.List;

public interface IAsyncTxRepository {

    /**
     * 初始化异步任务集合
     */
    void initialize(List<AsyncTxEvent> events);

    /**
     * 加载ready和running状态的任务
     * 
     * @param before 指定之前时间
     */
    List<AsyncTxEvent> loadReadyRunning(Duration before);

    /**
     * 标记任务成功
     */
    void markSuccess(AsyncTxEvent event);

    /**
     * 标记任务失败
     */
    void markFail(AsyncTxEvent event);

    /**
     * 标记任务运行中
     */
    void markRunning(AsyncTxEvent event);

    /**
     * 查询失败任务集合
     * 
     */
    List<AsyncTxEvent> queryFailed(int limit, Duration range);

}
