package com.cloud.arch.hotkey.net.push;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.net.admin.AdminClientHolder;
import com.cloud.arch.hotkey.utils.AsyncPool;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AdminServerPusher {

    private final LinkedBlockingQueue<HotKeyModel> pendingQueue = new LinkedBlockingQueue<>();

    public AdminServerPusher() {
        AsyncPool.asyncDo(this::pushToAdmin);
    }

    public void push(HotKeyModel model) {
        pendingQueue.offer(model);
    }

    private void pushToAdmin() {
        while (true) {
            while (!AdminClientHolder.isConnected()) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ignored) {
                }
            }
            try {
                final List<HotKeyModel> models = Lists.newArrayList();
                Queues.drain(pendingQueue, models, 512, 1, TimeUnit.SECONDS);
                if (CollectionUtils.isEmpty(models)) {
                    continue;
                }
                final String message = JSON.toJSONString(models);
                AdminClientHolder.push(message);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        }
    }

}
