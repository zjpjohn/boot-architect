package com.cloud.arch.transaction.core;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
@AllArgsConstructor
public class AsyncTxExecutor {

    private final Executor executor;
    private final IAsyncTxRepository repository;

    /**
     * 执行异步任务
     * 
     * @param event 异步任务事件
     */
    public void execute(AsyncTxEvent event) {
        String asyncKey = event.getAsyncKey();
        AsyncTxInvoker invoker = AsyncTxInvokers.get(asyncKey);
        if (invoker == null) {
            log.warn("异步任务key[{}]不存在，请检查任务配置。", asyncKey);
            return;
        }
        executor.execute(() -> {
            try {
                // 标记任务运行中
                repository.markRunning(event);
                // 执行任务
                invoker.invoke(event.getData());
                // 标记任务成功
                repository.markSuccess(event);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
                // 标记任务失败
                repository.markFail(event);
            }
        });
    }

}
