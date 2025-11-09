package com.cloud.arch.operate.core;

import com.cloud.arch.operate.props.OperateLogProperties;
import com.cloud.arch.trigger.BufferedTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;

@Slf4j
public class AsyncLogDispatcher implements DisposableBean {

    private static final Integer DEFAULT_KEEP_ALIVE = 1;

    private final ExecutorService             executor;
    private final BufferedTrigger<LogContext> bufferedTrigger;

    public AsyncLogDispatcher(OperateLogProperties properties, OperationLogHandle logHandle) {
        this.executor        = new ThreadPoolExecutor(properties.getCoreThreads(),
                                                      properties.getMaxThreads(),
                                                      DEFAULT_KEEP_ALIVE,
                                                      TimeUnit.MINUTES,
                                                      new SynchronousQueue<>(),
                                                      new NamedThreadFactory("async-log-thread-", false));
        this.bufferedTrigger = BufferedTrigger.<LogContext>builder()
                                              .consumer(logHandle)
                                              .executor(this.executor)
                                              .queue(new LinkedBlockingQueue<>())
                                              .batchSize(properties.getBatchSize())
                                              .timeout(properties.getTimeout())
                                              .build();
    }

    /**
     * 发布操作日志
     */
    public void publish(LogContext context) {
        this.bufferedTrigger.publish(context);
    }

    @Override
    public void destroy() throws Exception {
        bufferedTrigger.shutdown();
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

}
