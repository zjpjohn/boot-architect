package com.cloud.arch.support.core;

import com.cloud.arch.core.LogRecord;
import com.cloud.arch.props.OperateLoggerProperties;
import com.cloud.arch.repository.ILogRepository;
import com.cloud.arch.trigger.BufferedTrigger;
import com.cloud.arch.trigger.ConsumerListener;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class AsyncLogDispatcher implements DisposableBean, ConsumerListener<LogRecord> {

    private static final Integer DEFAULT_KEEP_ALIVE = 1;

    private final ExecutorService            executor;
    private final ILogRepository             logRepository;
    private final BufferedTrigger<LogRecord> bufferedTrigger;

    public AsyncLogDispatcher(ILogRepository logRepository, OperateLoggerProperties properties) {
        this.executor        = new ThreadPoolExecutor(properties.getCoreThreads(), properties.getMaxThreads(), DEFAULT_KEEP_ALIVE, TimeUnit.MINUTES, new SynchronousQueue<>());
        this.logRepository   = logRepository;
        this.bufferedTrigger = BufferedTrigger.<LogRecord>builder()
                                              .consumer(this)
                                              .executor(this.executor)
                                              .interval(properties.getTimeout())
                                              .queue(new LinkedBlockingQueue<>())
                                              .batchSize(properties.getBatchSize())
                                              .build();
    }

    @Override
    public void handle(List<LogRecord> records) {
        try {
            this.logRepository.save(records);
        } catch (Exception error) {
            log.error("async save the operate logs error:", error);
        }
    }

    /**
     * 发布操作日志
     */
    public void publishRecord(LogRecord logRecord) {
        Preconditions.checkNotNull(logRecord, "操作日志不允许为空.");
        this.bufferedTrigger.publish(logRecord);
    }

    @Override
    public void destroy() throws Exception {
        this.bufferedTrigger.shutdown();
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

}
