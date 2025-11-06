package com.cloud.arch.hotkey.detector;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.core.key.ReceiveNewKeyEvent;
import com.cloud.arch.hotkey.core.rule.KeyRuleManager;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.rule.KeyRule;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class HotKeyDetectWatcher {

    private final String                   appName;
    private final IConfigCenter            configCenter;
    private final KeyRuleManager           keyRuleManager;
    private final ExecutorService          executor;
    private final ScheduledExecutorService scheduler;

    public HotKeyDetectWatcher(String appName, IConfigCenter configCenter, KeyRuleManager keyRuleManager) {
        this.appName        = appName;
        this.configCenter   = configCenter;
        this.keyRuleManager = keyRuleManager;
        this.scheduler      = Executors.newSingleThreadScheduledExecutor();
        this.executor       = new ThreadPoolExecutor(2, 2, 300, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8));
    }

    /**
     * 探测监听器手动启动初始化
     */
    public void initialize() {
        //拉取热key规则，失败定时拉取
        this.fetchRuleWithRetry();
        //监听热key规则变化
        this.watchRuleChange();
        //监听手工添加的热key
        this.watchHotKey();
    }

    /**
     * 关闭监听线程池
     */
    public void dispose() {
        this.executor.shutdownNow();
        this.scheduler.shutdownNow();
    }

    /**
     * 从etcd中拉取热key规则配置,拉取失败进行定时重试
     */
    private void fetchRuleWithRetry() {
        scheduler.scheduleAtFixedRate(() -> {
            boolean result = this.fetchRuleFromEtcd();
            if (result) {
                this.fetchExistHotKey();
                scheduler.shutdown();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * 拉取已存在手工添加的热key
     */
    private void fetchExistHotKey() {
        String         prefixPath = ConfigConstant.hotKeyPath + appName;
        List<KeyValue> keyValues  = configCenter.getPrefix(prefixPath);
        for (KeyValue keyValue : keyValues) {
            //数据key截取：cacheName+'/'+key
            String      key      = keyValue.getKey().toStringUtf8().replace(prefixPath, "");
            String      value    = keyValue.getValue().toStringUtf8();
            String[]    splits   = key.split("/");
            HotKeyModel keyModel = new HotKeyModel();
            keyModel.setCache(splits[0]);
            keyModel.setKey(splits[1]);
            //如果本地缓存已经存在热点数据，控制台手动删除需向etcd中添加对应的key并设置删除标识
            keyModel.setRemove(HotkeyConstants.DEFAULT_DELETE_VALUE.equals(value));
            EventBusCenter.post(new ReceiveNewKeyEvent(keyModel));
        }
    }

    /**
     * etcd中热key路径-hotKeyPath/app/cache/key
     * 监听手工添加的热key
     */
    public void watchHotKey() {
        executor.submit(() -> {
            String prefix = ConfigConstant.hotKeyPath + appName;
            try (KvClient.WatchIterator iterator = configCenter.watchPrefix(prefix)) {
                while (iterator.hasNext()) {
                    Event       event    = iterator.next().getEvents().get(0);
                    KeyValue    keyValue = event.getKv();
                    String      value    = keyValue.getValue().toStringUtf8();
                    String      key      = keyValue.getKey().toStringUtf8().replace(prefix, "");
                    String[]    splits   = key.split("/");
                    HotKeyModel model    = new HotKeyModel();
                    model.setCache(splits[0]);
                    model.setKey(splits[1]);
                    //删除本地缓存热点数据，etcd删除事件或者控制台添加对应热点key的删除标识
                    boolean removed = event.getType() == Event.EventType.DELETE
                                      || HotkeyConstants.DEFAULT_DELETE_VALUE.equals(value);
                    model.setRemove(removed);
                    model.setGmtCreate(Long.parseLong(value));
                    EventBusCenter.post(new ReceiveNewKeyEvent(model));
                }
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        });
    }

    /**
     * 从etcd中拉取热key规则
     */
    private boolean fetchRuleFromEtcd() {
        try {
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.rulePath + appName);
            if (CollectionUtils.isEmpty(keyValues)) {
                keyRuleManager.putRules(Collections.emptyList());
                return true;
            }
            List<KeyRule> keyRules = keyValues.stream().map(kv -> {
                String value = kv.getValue().toStringUtf8();
                return JSON.parseObject(value, KeyRule.class);
            }).collect(Collectors.toList());
            keyRuleManager.putRules(keyRules);
            return true;
        } catch (StatusRuntimeException error) {
            log.error("Connected etcd failed. Please check etcd address.");
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return true;
        }
    }

    /**
     * 监听热key规则变化
     */
    private void watchRuleChange() {
        executor.submit(() -> {
            String prefix = ConfigConstant.rulePath + appName;
            try (KvClient.WatchIterator iterator = configCenter.watchPrefix(prefix)) {
                while (iterator.hasNext()) {
                    Event    event    = iterator.next().getEvents().get(0);
                    KeyValue keyValue = event.getKv();
                    String   value    = keyValue.getValue().toStringUtf8();
                    KeyRule  keyRule  = JSON.parseObject(value, KeyRule.class);
                    switch (event.getType()) {
                        case PUT:
                            keyRuleManager.putRule(keyRule);
                            break;
                        case DELETE:
                            keyRuleManager.removeRule(keyRule);
                            break;
                        default:
                    }
                }
            }
        });
    }
}
