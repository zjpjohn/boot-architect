package com.cloud.arch.hotkey.key;

import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.net.server.WorkerKeyReporter;
import com.cloud.arch.hotkey.utils.CpuNum;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class HotkeyDispatcher {
    /**
     * 缓冲队列
     */
    private final BlockingQueue<HotKeyModel> queue           = new LinkedBlockingQueue<>(2000000);
    private final ExecutorService            executorService = Executors.newCachedThreadPool();
    private final HotKeyProperties           properties;
    private final HotkeyProcessor            processor;
    private final WorkerKeyReporter          reporter;

    @Inject
    public HotkeyDispatcher(HotKeyProperties properties, HotkeyProcessor processor, WorkerKeyReporter reporter) {
        this.properties = properties;
        this.processor  = processor;
        this.reporter   = reporter;
        this.registerConsumers();
    }

    private void registerConsumers() {
        int workerCount = CpuNum.workerCount();
        if (properties.getThreadSize() > 0) {
            workerCount = properties.getThreadSize();
        } else {
            if (workerCount >= 8) {
                workerCount = workerCount / 2;
            }
        }
        for (int i = 0; i < workerCount; i++) {
            executorService.submit(this::handleHotKey);
        }
    }

    private void handleHotKey() {
        while (true) {
            try {

                HotKeyModel model = this.queue.take();
                if (model.isRemove()) {
                    processor.removeKey(model, KeyEventSource.CLIENT);
                } else {
                    processor.newKey(model, KeyEventSource.CLIENT);
                }
                reporter.incrDeal();
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        }
    }

    /**
     * 派发热key事件
     */
    public void dispatch(HotKeyModel model, long now) {
        if (model == null || model.getKey() == null) {
            return;
        }
        if (now - model.getGmtCreate() > this.properties.getExpireThreshold()) {
            this.reporter.incrExpire();
            return;
        }
        try {
            this.queue.put(model);
            this.reporter.incrOffer();
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }
    }

}
