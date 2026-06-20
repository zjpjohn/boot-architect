package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.metrics.EventStatsManager;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import com.cloud.arch.trigger.BufferedTrigger;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 事件标记批量处理器，基于 {@link BufferedTrigger} 的 drain-timeout 机制
 * 将多条单条标记合并为 batchUpdate，避免单条 UPDATE 的 DB 压力。
 *
 * <p>生产者直接将标记写入共享队列并唤醒触发任务；触发任务以阻塞排空方式积累
 * 条目，timeout 窗口内到达的标记会被合并为同一批次。
 */
public class BatchEventMarker implements DisposableBean {

    private final IDomainEventRepository     repository;
    private final BufferedTrigger<MarkEntry> trigger;
    private final ExecutorService            executor;
    private       EventStatsManager          statsManager = EventStatsManager.disabled();

    private record MarkEntry(PublishEventEntity entity, boolean succeeded) {
    }

    /**
     * @param repository 事件存储
     * @param batchSize  单次批量写入上限
     * @param interval   排空超时（ms），等同于窃取间隔
     */
    public BatchEventMarker(IDomainEventRepository repository, int batchSize, long interval) {
        this.repository = repository;
        this.executor = Executors.newSingleThreadExecutor(Threads.threadFactory("event-marker"));
        this.trigger = BufferedTrigger.<MarkEntry>builder()
                                      .executor(executor)
                                      .batchSize(batchSize)
                                      .timeout(Duration.ofMillis(interval))
                                      .consumer(this::flush)
                                      .build();
    }

    public void setEventStatsManager(EventStatsManager statsManager) {
        if (statsManager != null) {
            this.statsManager = statsManager;
        }
    }

    public void markSucceeded(PublishEventEntity entity) {
        trigger.publish(new MarkEntry(entity, true));
    }

    public void markFailed(PublishEventEntity entity) {
        trigger.publish(new MarkEntry(entity, false));
    }

    private void flush(List<MarkEntry> entries) {
        statsManager.recordBatchMarkSize(entries.size());
        List<PublishEventEntity> succeeded = new ArrayList<>(entries.size());
        List<PublishEventEntity> failed    = new ArrayList<>(entries.size());
        for (MarkEntry entry : entries) {
            if (entry.succeeded) {
                succeeded.add(entry.entity);
            } else {
                failed.add(entry.entity);
            }
        }
        if (!succeeded.isEmpty()) {
            try {
                repository.batchMarkSucceeded(succeeded);
                statsManager.recordBatchMarkSucceeded();
            } catch (Exception e) {
                statsManager.recordBatchMarkFailed();
            }
        }
        if (!failed.isEmpty()) {
            try {
                repository.batchMarkFailed(failed, null);
                statsManager.recordBatchMarkSucceeded();
            } catch (Exception e) {
                statsManager.recordBatchMarkFailed();
            }
        }
    }

    @Override
    public void destroy() {
        trigger.shutdown();
        executor.shutdownNow();
    }
}
