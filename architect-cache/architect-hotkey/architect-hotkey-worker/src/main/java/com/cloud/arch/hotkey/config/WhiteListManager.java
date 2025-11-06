package com.cloud.arch.hotkey.config;

import com.cloud.arch.hotkey.utils.AsyncPool;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class WhiteListManager {

    private final Multimap<String, String> whiteList = HashMultimap.create();

    private final IConfigCenter configCenter;

    @Inject
    public WhiteListManager(IConfigCenter configCenter) {
        this.configCenter = configCenter;
        this.watchWhiteList();
    }

    private void watchWhiteList() {
        AsyncPool.asyncDo(() -> {
            this.fetchWhiteList();
            KvClient.WatchIterator watchIterator = configCenter.watch(ConfigConstant.whiteListPath);
            while (watchIterator.hasNext()) {
                WatchUpdate update   = watchIterator.next();
                Event       event    = update.getEvents().get(0);
                KeyValue    keyValue = event.getKv();
                String      appName  = keyValue.getKey().toStringUtf8().replace(ConfigConstant.whiteListPath, "");
                if (StringUtils.isNotBlank(appName)) {
                    String[] split = keyValue.getValue().toStringUtf8().split(",");
                    whiteList.putAll(appName, Arrays.asList(split));
                }
            }
        });
    }

    private void fetchWhiteList() {
        List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.whiteListPath);
        for (KeyValue keyValue : keyValues) {
            String appName = keyValue.getKey().toStringUtf8().replace(ConfigConstant.whiteListPath, "");
            if (StringUtils.isBlank(appName)) {
                continue;
            }
            String[] split = keyValue.getValue().toStringUtf8().split(",");
            whiteList.putAll(appName, Arrays.asList(split));
        }

    }

    public boolean contains(String appName, String key) {
        return this.whiteList.containsEntry(appName, key);
    }

}
