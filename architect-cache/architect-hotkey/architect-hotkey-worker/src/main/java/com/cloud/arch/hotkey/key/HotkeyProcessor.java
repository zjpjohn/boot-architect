package com.cloud.arch.hotkey.key;

import cn.hutool.core.date.SystemClock;
import com.cloud.arch.hotkey.cache.CacheBuilder;
import com.cloud.arch.hotkey.cache.CacheManager;
import com.cloud.arch.hotkey.config.LoggerManager;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.net.push.AdminServerPusher;
import com.cloud.arch.hotkey.net.push.AppServerPusher;
import com.cloud.arch.hotkey.rule.KeyRule;
import com.cloud.arch.hotkey.utils.SlidingWindow;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class HotkeyProcessor {

    private static final String SPLITTER         = "-";
    private static final String NEW_KEY_EVENT    = "new key created event, key : {}";
    private static final String DELETE_KEY_EVENT = "key delete event key : {}";


    private final Cache<String, Object> hotCache;
    private final AppServerPusher       appServerPusher;
    private final AdminServerPusher     adminServerPusher;
    private final CacheManager          cacheManager;
    private final LoggerManager         loggerManager;
    private final KeyRuleManager        keyRuleManager;

    @Inject
    public HotkeyProcessor(CacheManager cacheManager,
                           LoggerManager loggerManager,
                           KeyRuleManager keyRuleManager,
                           AppServerPusher appServerPusher,
                           AdminServerPusher adminServerPusher) {
        this.hotCache          = CacheBuilder.buildRecentHotKeyCache();
        this.loggerManager     = loggerManager;
        this.appServerPusher   = appServerPusher;
        this.adminServerPusher = adminServerPusher;
        this.cacheManager      = cacheManager;
        this.keyRuleManager    = keyRuleManager;
    }

    /**
     * 新key计算热key
     *
     * @param model  热key信息
     * @param source 事件来源
     */
    void newKey(HotKeyModel model, KeyEventSource source) {
        String key = buildKey(model);
        if (hotCache.getIfPresent(key) != null) {
            return;
        }
        final KeyRule rule = keyRuleManager.getRuleByHotKey(model);
        if (rule == null) {
            return;
        }
        SlidingWindow         slidingWindow = checkWindow(model, rule, key);
        boolean               isHot         = slidingWindow.addCount(model.getCount());
        Cache<String, Object> cache         = cacheManager.getCache(model.getAppName(), model.getCache());
        if (!isHot) {
            cache.put(key, slidingWindow);
            return;
        }
        hotCache.put(key, 1);
        //已成为热key，清空
        cache.invalidate(key);
        //设置推送事件
        model.setGmtCreate(SystemClock.now());
        //打印日志
        if (loggerManager.isOn()) {
            log.info(NEW_KEY_EVENT, model.getKey());
        }
        //推送热key到APP应用
        appServerPusher.push(model);
        //推送热key到admin管理应用
        adminServerPusher.push(model);
    }

    /**
     * 删除热key
     */
    void removeKey(HotKeyModel model, KeyEventSource source) {
        String key = buildKey(model);
        //清空已缓存的热key
        hotCache.invalidate(key);
        //清空热key时间窗信息
        cacheManager.getCache(model.getAppName(), model.getCache()).invalidate(key);
        model.setGmtCreate(SystemClock.now());
        //日志开关
        if (loggerManager.isOn()) {
            log.info(DELETE_KEY_EVENT, model.getKey());
        }
        //推送删除热key
        appServerPusher.remove(model);
    }

    /**
     * 生成或返回该key的滑窗
     */
    private SlidingWindow checkWindow(HotKeyModel hotKeyModel, KeyRule rule, String key) {
        //取该key的滑窗
        return (SlidingWindow) cacheManager.getCache(hotKeyModel.getAppName(), hotKeyModel.getCache())
                                           .get(key, (Function<String, SlidingWindow>) s -> new SlidingWindow(rule.getInterval(), rule.getThreshold()));
    }

    private String buildKey(HotKeyModel hotKeyModel) {
        return Joiner.on(SPLITTER)
                     .join(hotKeyModel.getAppName(), hotKeyModel.getKeyType(), hotKeyModel.getCache(), hotKeyModel.getKey());
    }

}
