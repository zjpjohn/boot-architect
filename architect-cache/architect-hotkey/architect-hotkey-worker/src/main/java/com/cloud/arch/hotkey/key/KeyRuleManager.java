package com.cloud.arch.hotkey.key;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.cache.CacheManager;
import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.rule.KeyRule;
import com.cloud.arch.hotkey.utils.AsyncPool;
import com.google.common.collect.Maps;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Slf4j
public class KeyRuleManager {

    private final Map<String, KeyRule> ruleCache = Maps.newConcurrentMap();

    private final CacheManager          cacheManager;
    private final IConfigCenter         configCenter;
    private final WorkerServerScheduler scheduler;


    @Inject
    public KeyRuleManager(CacheManager cacheManager, IConfigCenter configCenter, WorkerServerScheduler scheduler) {
        this.cacheManager = cacheManager;
        this.configCenter = configCenter;
        this.scheduler    = scheduler;
    }

    /**
     * 监听缓存规则变化
     */
    public void watchRuleChange() {
        AsyncPool.asyncDo(() -> {
            try {
                KvClient.WatchIterator iterator = configCenter.watchPrefix(ConfigConstant.rulePath);
                while (iterator.hasNext()) {
                    Event    event    = iterator.next().getEvents().get(0);
                    KeyValue keyValue = event.getKv();
                    String   ruleJson = keyValue.getValue().toStringUtf8();
                    KeyRule  rule     = JSON.parseObject(ruleJson, KeyRule.class);
                    if (event.getType() == Event.EventType.PUT) {
                        this.put(rule);
                    } else if (event.getType() == Event.EventType.DELETE) {
                        this.remove(rule);
                    }

                }
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        });
    }

    /**
     * 定期拉取缓存规则
     */
    public void schedulePullRules() {
        scheduler.schedule(0, 60L, () -> {
            List<KeyValue> values = configCenter.getPrefix(ConfigConstant.rulePath);
            for (KeyValue value : values) {
                String  ruleJson = value.getValue().toStringUtf8();
                KeyRule rule     = JSON.parseObject(ruleJson, KeyRule.class);
                this.put(rule);
            }
        });
    }

    /**
     * 根据规则的缓存组名称获取规则集合
     *
     * @param appName 应用名称
     * @param cache   缓存名称
     */
    private KeyRule ofKeyRule(String appName, String cache) {
        String ruleKey = this.ruleKey(appName, cache);
        return ruleCache.get(ruleKey);
    }

    /**
     * 更新规则，更新成功返回true，未更新返回false
     *
     * @param rule 应用缓存规则
     */
    public boolean update(KeyRule rule) {
        String  ruleKey = this.ruleKey(rule.getApp(), rule.getCache());
        KeyRule keyRule = ruleCache.get(ruleKey);
        if (keyRule == null || !keyRule.toString().equals(rule.toString())) {
            ruleCache.put(ruleKey, rule);
            return true;
        }
        return false;
    }

    /**
     * 查询热key的规则
     *
     * @param model 热key信息
     */
    public KeyRule getRuleByHotKey(HotKeyModel model) {
        return this.ofKeyRule(model.getAppName(), model.getCache());
    }

    /**
     * 更新应用缓存热key规则
     *
     * @param rule 规则信息
     */
    public void put(KeyRule rule) {
        boolean result = this.update(rule);
        //规则更新成功，需要清空对应的APP热key判断缓存
        if (result) {
            cacheManager.clearCache(rule.getApp(), rule.getCache());
        }
    }

    /**
     * 删除缓存热key规则
     */
    public void remove(KeyRule rule) {
        String ruleKey = this.ruleKey(rule.getApp(), rule.getCache());
        if (ruleCache.remove(ruleKey) != null) {
            this.cacheManager.clearCache(rule.getApp(), rule.getCache());
        }
    }

    private String ruleKey(String app, String cache) {
        return app + "/" + cache;
    }
}
