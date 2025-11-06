package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.core.rule.KeyRuleManager;
import com.cloud.arch.hotkey.enums.KeyType;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.rule.KeyRule;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Slf4j
public class HotKeyCache {

    private final String                                    appName;
    private final KeyRuleManager                            keyRuleManager;
    private final IKeyCollector<HotKeyModel, HotKeyModel>   hotKeyCollector;
    private final IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector;

    public HotKeyCache(String appName,
                       KeyRuleManager keyRuleManager,
                       IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                       IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        this.appName           = appName;
        this.keyRuleManager    = keyRuleManager;
        this.hotKeyCollector   = hotKeyCollector;
        this.keyCountCollector = keyCountCollector;
    }

    /**
     * 清除缓存组的全部数据
     *
     * @param name 缓存名称
     */
    public void removeAll(String name) {
        Optional.ofNullable(keyRuleManager.ofCache(name)).ifPresent(Cache::invalidateAll);
    }

    /**
     * 删除指定的key
     *
     * @param key 热key标识
     */
    public void remove(String name, String key) {
        Optional.ofNullable(keyRuleManager.ofCache(name)).ifPresent(cache -> cache.invalidate(key));
    }

    /**
     * 进行热key填充，标记此key为热key
     *
     * @param key 热key标识
     */
    public void fillHotKey(String name, String key) {
        KeyRule                     keyRule = keyRuleManager.findRule(name);
        final Cache<String, Object> cache   = keyRuleManager.ofCache(name);
        if (keyRule != null && cache != null) {
            //标记缓存为热key，但是缓存数据为填充
            cache.put(key, ValueModel.mark(keyRule.getDuration()));
        }
    }

    /**
     * 更新热key对应缓存值
     *
     * @param key   热key
     * @param value 热key缓存值
     */
    public void put(String name, String key, Object value) {
        KeyRule               rule  = keyRuleManager.findRule(name);
        Cache<String, Object> cache = keyRuleManager.ofCache(name);
        if (rule != null && cache != null) {
            cache.put(key, ValueModel.value(value, rule.getDuration()));
        }
    }

    /**
     * 判断key是否为热key
     *
     * @param key 请求key
     */
    public boolean isHotKey(String name, String key) {
        try {
            KeyRule               keyRule = keyRuleManager.findRule(name);
            Cache<String, Object> cache   = keyRuleManager.ofCache(name);
            if (keyRule == null || cache == null) {
                return false;
            }
            ValueModel result = (ValueModel) cache.getIfPresent(key);
            boolean    isHot  = result != null;
            //非热key或者是热key数据快要过期，推送热key计算事件
            if (!isHot || isNearExpire(result)) {
                this.hotKeyCollect(name, key, 1);
            }
            keyCountCollector.collect(new KeyHotModel(key, name, isHot));
            return isHot;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 判断key是否是热key并获取返回值
     *
     * @param key key标识
     */
    public DetectedValue get(String name, String key) {
        DetectedValue valueWrapper = new DetectedValue(key, true);
        try {
            Cache<String, Object> cache = keyRuleManager.ofCache(name);
            KeyRule               rule  = keyRuleManager.findRule(name);
            if (cache == null || rule == null) {
                valueWrapper.setHot(false);
                return valueWrapper;
            }
            ValueModel result = (ValueModel) cache.getIfPresent(key);
            //判断当前值是否为热key
            final boolean hottedValue = result != null;
            valueWrapper.setHot(hottedValue);
            if (!hottedValue) {
                //当前key不是热key，收集key信息
                this.hotKeyCollect(name, key, 1);
            } else {
                Object userValue = result.getValue();
                if (userValue instanceof Integer && HotkeyConstants.MAGIC_NUMBER == (int) userValue) {
                    userValue = null;
                }
                valueWrapper.setCached(result.isFilled());
                valueWrapper.setValue(userValue);
                //热key数据临近过期，收集探测信息
                if (isNearExpire(result)) {
                    this.hotKeyCollect(name, key, 1);
                }
            }
            //收集key规则统计信息
            keyCountCollector.collect(new KeyHotModel(key, name, hottedValue));
        } catch (Exception e) {
            valueWrapper.setSuccess(false);
            log.error(e.getMessage(), e);
        }
        return valueWrapper;
    }


    /**
     * 是否临近过期
     */
    private static boolean isNearExpire(ValueModel valueModel) {
        //判断是否过期时间小于1秒，小于1秒的话也发送
        if (valueModel == null) {
            return true;
        }
        return valueModel.getCreateTime() + valueModel.getDuration() - System.currentTimeMillis() <= 2000;
    }

    /**
     * 推送探测数据
     *
     * @param name  缓存分组信息
     * @param key   待探测热key
     * @param count 统计次数
     */
    public void hotKeyCollect(String name, String key, int count) {
        Preconditions.checkState(StringUtils.isNotBlank(name), "name不允许为空.");
        Preconditions.checkState(StringUtils.isNotBlank(key), "key不允许为空.");
        //当前缓存是否在探测规则内，本地缓存定时推送至worker进行分析
        if (!keyRuleManager.isExist(name)) {
            return;
        }
        HotKeyModel model = new HotKeyModel();
        model.setKey(key);
        model.setKeyType(KeyType.REDIS_KEY);
        model.setCount(Math.max(1, count));
        model.setCache(name);
        model.setAppName(this.appName);
        model.setRemove(false);
        hotKeyCollector.collect(model);
    }

}
