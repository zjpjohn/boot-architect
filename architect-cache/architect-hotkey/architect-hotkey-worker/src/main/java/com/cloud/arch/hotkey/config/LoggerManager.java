package com.cloud.arch.hotkey.config;


import com.cloud.arch.hotkey.utils.AsyncPool;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
public class LoggerManager {

    private volatile Boolean       loggerOn = true;
    private final    IConfigCenter configCenter;

    @Inject
    public LoggerManager(IConfigCenter configCenter) {
        this.configCenter = configCenter;
        this.watchLogger();
    }

    private void watchLogger() {
        AsyncPool.asyncDo(() -> {
            try {
                String switcher = configCenter.get(ConfigConstant.logToggle);
                this.loggerOn = "true".equals(switcher) | "1".equals(switcher);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            KvClient.WatchIterator watchIterator = configCenter.watch(ConfigConstant.logToggle);
            while (watchIterator.hasNext()) {
                WatchUpdate watchUpdate = watchIterator.next();
                List<Event> events      = watchUpdate.getEvents();
                KeyValue    keyValue    = events.get(0).getKv();
                String      value       = keyValue.getValue().toStringUtf8();
                this.loggerOn = "true".equals(value) || "1".equals(value);
            }
        });
    }

    /**
     * 是否开启日志
     */
    public boolean isOn() {
        return this.loggerOn;
    }

}
