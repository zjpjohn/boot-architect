package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.model.HotKeyModel;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TurnKeyCollector implements IKeyCollector<HotKeyModel, HotKeyModel> {

    private final ConcurrentHashMap<String, HotKeyModel> cache0 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HotKeyModel> cache1 = new ConcurrentHashMap<>();
    private final AtomicLong                             cursor = new AtomicLong(0);

    /**
     * 锁定后返回数据
     */
    @Override
    public List<HotKeyModel> lockAndGet() {
        cursor.addAndGet(1);
        ConcurrentHashMap<String, HotKeyModel> map    = cursor.get() % 2 == 0 ? cache1 : cache0;
        List<HotKeyModel>                      models = Lists.newArrayList(map.values());
        map.clear();
        return models;
    }

    /**
     * 收集输入的参数
     */
    @Override
    public void collect(HotKeyModel model) {
        ConcurrentHashMap<String, HotKeyModel> map      = cursor.get() % 2 == 0 ? cache0 : cache1;
        HotKeyModel                            keyModel = map.putIfAbsent(model.getKey(), model);
        if (keyModel != null) {
            keyModel.addCount(model.getCount());
        }
    }

    @Override
    public void finishOnce() {

    }
}
