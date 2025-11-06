package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.detector.EventBusCenter;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReceiveNewKeySubscriber {

    private final HotKeyCache hotKeyCache;

    public ReceiveNewKeySubscriber(HotKeyCache hotKeyCache) {
        this.hotKeyCache = hotKeyCache;
        EventBusCenter.register(this);
    }

    @Subscribe
    public void subscribe(ReceiveNewKeyEvent event) {
        HotKeyModel keyModel = event.getModel();
        if (keyModel == null) {
            return;
        }
        long   now    = System.currentTimeMillis();
        String hotKey = keyModel.getKey();
        if (keyModel.getGmtCreate() != null && Math.abs(now - keyModel.getGmtCreate()) > 1000) {
            log.warn("hot key [{}] comes too late, now {} createTime {}", hotKey, now, keyModel.getGmtCreate());
        }
        //删除事件处理
        if (keyModel.isRemove()) {
            hotKeyCache.remove(keyModel.getCache(), hotKey);
            return;
        }
        //已经是热key，再次推送过来，打印日志并刷新缓存值
        if (hotKeyCache.isHotKey(keyModel.getCache(), hotKey)) {
            log.info("receive repeat hot key [{}] at {}", hotKey, now);
        }
        //设置为热key，但是没有加载缓存数据，缓存数据设置由第三方扩展填充
        hotKeyCache.fillHotKey(keyModel.getCache(), hotKey);
    }
}
