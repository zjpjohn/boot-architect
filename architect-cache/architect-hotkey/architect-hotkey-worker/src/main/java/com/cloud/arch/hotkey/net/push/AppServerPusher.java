package com.cloud.arch.hotkey.net.push;

import cn.hutool.core.collection.CollectionUtil;
import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.net.model.AppInfo;
import com.cloud.arch.hotkey.net.server.HotkeyClientHolder;
import com.cloud.arch.hotkey.utils.AsyncPool;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class AppServerPusher {

    private final LinkedBlockingQueue<HotKeyModel> queue = new LinkedBlockingQueue<>();

    private final HotkeyClientHolder hotkeyClientHolder;

    @Inject
    public AppServerPusher(HotkeyClientHolder hotkeyClientHolder) {
        this.hotkeyClientHolder = hotkeyClientHolder;
        this.asyncBatchPush();
    }

    private void asyncBatchPush() {
        AsyncPool.asyncDo(() -> {
            while (true) {
                try {
                    List<HotKeyModel> tempModels = Lists.newArrayList();
                    Queues.drain(queue, tempModels, 10, 10, TimeUnit.MILLISECONDS);
                    if (CollectionUtil.isEmpty(tempModels)) {
                        continue;
                    }
                    Map<String, List<HotKeyModel>> groupModels = tempModels.stream()
                                                                           .collect(Collectors.groupingBy(HotKeyModel::getAppName));
                    for (AppInfo appInfo : hotkeyClientHolder.getAppList()) {
                        List<HotKeyModel> models = groupModels.get(appInfo.getName());
                        if (CollectionUtil.isEmpty(models)) {
                            continue;
                        }
                        HotkeyCommand command = new HotkeyCommand(MessageType.RESPONSE_NEW_KEY);
                        command.setHotKeyModels(models);
                        //推送至整个APP应用
                        appInfo.push(command);
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 推送热key到APP应用
     *
     * @param model 热key信息
     */
    public void push(HotKeyModel model) {
        this.queue.offer(model);
    }

    /**
     * 删除热key信息
     *
     * @param model 热key信息
     */
    public void remove(HotKeyModel model) {
        this.queue.offer(model);
    }

}
