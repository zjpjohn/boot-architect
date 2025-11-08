package com.cloud.arch.trigger;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.utils.SleepyTask;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BufferedTrigger<E> {

    /**
     * 缓存队列
     */
    private BlockingQueue<E>    queue;
    /**
     * 消费监听处理器
     */
    private ConsumerListener<E> consumer;
    /**
     * 执行线程池
     */
    private ExecutorService     executor;
    /**
     * 单次处理数量
     */
    private int                 batchSize;
    /**
     * 触发时间间隔
     */
    private Duration            interval;
    /**
     * 消费者出发策略
     */
    private TriggerStrategy     strategy;

    private BufferedTrigger() {
    }

    private BufferedTrigger(BlockingQueue<E> queue,
                            ConsumerListener<E> consumer,
                            ExecutorService executor,
                            int batchSize,
                            Duration interval) {
        this.queue     = queue;
        this.consumer  = consumer;
        this.executor  = executor;
        this.batchSize = batchSize;
        this.interval  = interval;
    }

    /**
     * 发布单个事件
     */
    public void publish(E event) {
        this.queue.add(event);
        this.strategy.wakeUp();
    }

    /**
     * 批量发布事件
     */
    public void publish(List<E> events) {
        this.queue.addAll(events);
        this.strategy.wakeUp();
    }

    /**
     * 关闭任务
     */
    public void shutdown() {
        this.strategy.shutdown();
    }

    /**
     * 单消费者策略
     */
    private BufferedTrigger<E> single() {
        this.strategy = new SingleStrategy<>(this);
        return this;
    }

    /**
     * 多消费者策略
     */
    private BufferedTrigger<E> multi(Integer size) {
        this.strategy = new MultiStrategy<>(size, this);
        return this;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<E> {
        /**
         * 缓存队列
         */
        private BlockingQueue<E>    queue     = Queues.newLinkedBlockingQueue();
        /**
         * 消费监听处理器
         */
        private ConsumerListener<E> listener;
        /**
         * 执行线程池
         */
        private ExecutorService     executor  = Executors.newSingleThreadExecutor();
        /**
         * 单次处理数量,默认20
         */
        private int                 batchSize = 20;
        /**
         * 触发时间间隔，默认5秒
         */
        private Duration            interval  = Duration.ofSeconds(5);
        /**
         * 消费者数量，默认单消费者
         */
        private Integer             consumers = 1;

        public Builder<E> queue(BlockingQueue<E> queue) {
            this.queue = Preconditions.checkNotNull(queue);
            return this;
        }

        public Builder<E> consumer(ConsumerListener<E> listener) {
            this.listener = Preconditions.checkNotNull(listener);
            return this;
        }

        public Builder<E> executor(ExecutorService executor) {
            this.executor = Preconditions.checkNotNull(executor);
            return this;
        }

        public Builder<E> batchSize(int batchSize) {
            Preconditions.checkState(consumers >= 1, "批次数量不小于1");
            this.batchSize = batchSize;
            return this;
        }

        public Builder<E> interval(Duration interval) {
            this.interval = Preconditions.checkNotNull(interval);
            return this;
        }

        public Builder<E> consumers(Integer consumers) {
            Preconditions.checkState(consumers >= 1, "消费者数量不小于1");
            this.consumers = consumers;
            return this;
        }

        public BufferedTrigger<E> build() {
            BufferedTrigger<E> trigger = new BufferedTrigger<>(this.queue,
                                                               this.listener,
                                                               this.executor,
                                                               this.batchSize,
                                                               this.interval);
            if (consumers == 1) {
                return trigger.single();
            }
            return trigger.multi(this.consumers);
        }
    }

    /**
     * 单一消费者策略
     */
    public static class SingleStrategy<E> implements TriggerStrategy {

        private final TriggerTask<E> triggerTask;

        public SingleStrategy(BufferedTrigger<E> trigger) {
            this.triggerTask = new TriggerTask<>(trigger);
        }

        @Override
        public void wakeUp() {
            this.triggerTask.wakeup();
        }

        @Override
        public void shutdown() {
            triggerTask.shutdown();
        }
    }

    /**
     * 多消费者策略
     */
    public static class MultiStrategy<E> implements TriggerStrategy {

        private final Integer              size;
        private final BufferedTrigger<E>   trigger;
        private final List<TriggerTask<E>> tasks;

        public MultiStrategy(Integer size, BufferedTrigger<E> trigger) {
            this.size    = size;
            this.trigger = trigger;
            this.tasks   = this.buildTasks();
        }

        private List<TriggerTask<E>> buildTasks() {
            List<TriggerTask<E>> tasks = Lists.newArrayList();
            for (int i = 0; i < size; i++) {
                tasks.add(new TriggerTask<>(trigger));
            }
            return tasks;
        }

        @Override
        public void wakeUp() {
            this.tasks.stream().filter(e -> !e.isRunning()).findFirst().orElseGet(tasks::getFirst).wakeup();
        }

        @Override
        public void shutdown() {
            this.tasks.forEach(TriggerTask::shutdown);
        }
    }

    /**
     * 触发任务
     */
    @Slf4j
    public static class TriggerTask<E> extends SleepyTask {

        private final BufferedTrigger<E> trigger;

        public TriggerTask(BufferedTrigger<E> trigger) {
            super(trigger.executor);
            this.trigger = trigger;
        }

        @Override
        protected void runTask() {
            while (!this.trigger.queue.isEmpty()) {
                try {
                    List<E> events  = Lists.newArrayList();
                    int     drained = Queues.drain(this.trigger.queue, events, trigger.batchSize, trigger.interval);
                    if (drained > 0 && CollectionUtils.isNotEmpty(events)) {
                        this.trigger.consumer.handle(events);
                    }
                } catch (Exception error) {
                    log.error(error.getMessage(), error);
                }
            }
        }
    }
}
