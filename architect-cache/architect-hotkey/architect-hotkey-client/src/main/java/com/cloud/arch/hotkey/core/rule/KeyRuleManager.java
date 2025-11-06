package com.cloud.arch.hotkey.core.rule;

import com.cloud.arch.hotkey.rule.KeyRule;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class KeyRuleManager {

    /**
     * cacheName和caffeine的映射{cacheName:caffeine}
     */
    private final Map<String, Cache<String, Object>> cacheMap  = Maps.newConcurrentMap();
    /**
     * 规则缓存集合{cacheName:rule}
     */
    private final Map<String, KeyRule>               ruleCache = Maps.newConcurrentMap();

    /**
     * 更新所有规则，重建缓存
     *
     * @param rules 规则集合
     */
    public void putRules(List<KeyRule> rules) {
        synchronized (ruleCache) {
            ruleCache.clear();
            if (CollectionUtils.isEmpty(rules)) {
                cacheMap.clear();
            }
            //重建规则缓存
            rules.forEach(rule -> ruleCache.put(rule.getCache(), rule));
            //清除不在规则中的缓存信息
            cacheMap.keySet().stream().filter(key -> !ruleCache.containsKey(key)).forEach(cacheMap::remove);
            //构建新增规则缓存
            ruleCache.values().stream().filter(rule -> !cacheMap.containsKey(rule.getCache())).forEach(rule -> {
                final Cache<String, Object> cache = CacheBuilder.cache(rule);
                cacheMap.put(rule.getCache(), cache);
            });
        }
    }

    /**
     * 更新缓存热点key规则以及重建热点数据缓存数据
     */
    public void putRule(KeyRule rule) {
        if (ruleCache.containsKey(rule.getCache())) {
            ruleCache.remove(rule.getCache());
            cacheMap.remove(rule.getCache());
        }
        ruleCache.put(rule.getCache(), rule);
        cacheMap.put(rule.getCache(), CacheBuilder.cache(rule));
    }

    /**
     * 删除指定缓存规则信息以及热点数据缓存
     *
     * @param rule 缓存热点key规则信息
     */
    public void removeRule(KeyRule rule) {
        if (ruleCache.containsKey(rule.getCache())) {
            ruleCache.remove(rule.getCache());
            cacheMap.remove(rule.getCache());
        }
    }

    /**
     * 根据缓存名称查询对应的热key规则
     *
     * @param name 缓存名称
     */
    public KeyRule findRule(String name) {
        return this.ruleCache.get(name);
    }

    /**
     * 获取指定缓存名称的缓存
     *
     * @param name 缓存名称
     */
    public Cache<String, Object> ofCache(String name) {
        return cacheMap.get(name);
    }

    /**
     * 查询缓存规则对应的缓存集合
     *
     * @param rule 缓存规则
     */
    public Cache<String, Object> ofCache(KeyRule rule) {
        return cacheMap.get(rule.getCache());
    }

    /**
     * 判断缓存是否在探测的规则内
     *
     * @param name 缓存名称
     */
    public boolean isExist(String name) {
        return this.ruleCache.containsKey(name);
    }

}
