package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.AsyncTxInvoker;
import com.cloud.arch.transaction.core.AsyncTxInvokers;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AsyncRetryQueue implements SmartInitializingSingleton, DisposableBean {

    private static final long DEFAULT_LOAD_UNTIL_TIME = TimeUnit.HOURS.toMillis(1);

    private final AtomicBoolean              startState  = new AtomicBoolean(false);
    private final Set<AsyncTxEvent>          retryEvents = Sets.newConcurrentHashSet();
    private final DelayQueue<AsyncRetryTask> delayQueue  = new DelayQueue<>();
    private final IAsyncTxRepository         repository;
    private final Executor                   executor;
    private final Thread                     delayWorker;

    public AsyncRetryQueue(Executor executor, IAsyncTxRepository repository) {
        this.executor    = executor;
        this.repository  = repository;
        this.delayWorker = new Thread(this::triggerDelayWorker);
    }

    private void triggerDelayWorker() {
        do {
            try {
                AsyncRetryTask task = this.delayQueue.poll(DEFAULT_LOAD_UNTIL_TIME, TimeUnit.MILLISECONDS);
                while (task != null) {
                    this.execute(task.event());
                    task = this.delayQueue.take();
                }
            } catch (InterruptedException ignore) {
            }
        }
        while (this.startState.get());
    }

    private void execute(AsyncTxEvent event) {
        String         asyncKey = event.getAsyncKey();
        AsyncTxInvoker invoker  = AsyncTxInvokers.get(asyncKey);
        if (invoker == null) {
            log.warn("异步任务key[{}]不存在，请检查任务配置。", asyncKey);
            return;
        }
        executor.execute(() -> {
            try {
                // 移除已经执行的事件
                retryEvents.remove(event);
                // 递增重试次数
                event.incrementRetry();
                // 执行任务
                invoker.invokeRetry(event);
                // 标记任务成功
                repository.markSuccess(event);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
                repository.markFail(event);
            }
        });
    }

    /**
     * 添加单个延迟事件
     */
    public void delay(AsyncTxEvent event) {
        if (!retryEvents.contains(event)) {
            retryEvents.add(event);
            this.delayQueue.add(new AsyncRetryTask(event));
        }
    }

    /**
     * 批量添加延迟事件
     */
    public void delay(Collection<AsyncTxEvent> events) {
        if (CollectionUtils.isNotEmpty(events)) {
            List<AsyncRetryTask> retryTasks = events.stream()
                                                    .filter(event -> !retryEvents.contains(event))
                                                    .map(AsyncRetryTask::new)
                                                    .toList();
            this.retryEvents.addAll(events);
            this.delayQueue.addAll(retryTasks);
        }
    }

    @Override
    public void destroy() throws Exception {
        this.startState.set(false);
        if (!this.delayWorker.isInterrupted()) {
            this.delayWorker.interrupt();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (this.startState.compareAndSet(false, true)) {
            this.delayWorker.start();
        }
    }

}
