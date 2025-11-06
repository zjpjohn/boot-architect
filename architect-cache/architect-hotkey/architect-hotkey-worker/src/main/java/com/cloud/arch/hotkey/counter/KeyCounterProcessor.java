package com.cloud.arch.hotkey.counter;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.utils.AsyncPool;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.cloud.arch.hotkey.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class KeyCounterProcessor {

    private final LinkedBlockingQueue<KeyCountItem> counterQueue = new LinkedBlockingQueue<>();
    private final IConfigCenter                     configCenter;

    @Inject
    public KeyCounterProcessor(IConfigCenter configCenter) {
        this.configCenter = configCenter;
        this.registerHandle();
    }

    private void registerHandle() {
        AsyncPool.asyncDo(() -> {
            Map<String, String> cache = new HashMap<>(500);
            while (true) {
                try {
                    KeyCountItem item = counterQueue.take();
                    //每个List是一个client的10秒内的数据，一个rule如果每秒都有数据，那list里就有10条
                    List<KeyCountModel> keyCountModels = item.getList();
                    String              appName        = item.getAppName();
                    for (KeyCountModel keyCountModel : keyCountModels) {
                        //rule_key(规则名称)#**#group(缓存分组)#**#timestamp(时间戳:2022-11-07 18:35:23)
                        String ruleKey       = keyCountModel.getRuleKey();
                        int    hotHitCount   = keyCountModel.getHotHitCount();
                        int    totalHitCount = keyCountModel.getTotalHitCount();
                        String mapKey        = appName + HotkeyConstants.COUNT_DELIMITER + ruleKey;
                        if (cache.get(mapKey) == null) {
                            cache.put(mapKey, hotHitCount + "-" + totalHitCount);
                        } else {
                            String[] counts     = cache.get(mapKey).split("-");
                            int      hotCount   = Integer.parseInt(counts[0]) + hotHitCount;
                            int      totalCount = Integer.parseInt(counts[1]) + totalHitCount;
                            cache.put(mapKey, hotCount + "-" + totalCount);
                        }
                    }
                    //300是什么意思呢？300就代表了300秒的数据了，已经不少了
                    if (cache.size() >= 300) {
                        //key：ConfigConstant.keyHitCountPath + appName + "/" + IpUtils.getIp() + "-" + System.currentTimeMillis()
                        String configKey = ConfigConstant.keyHitCountPath
                                           + appName
                                           + "/"
                                           + IpUtils.getIp()
                                           + "-"
                                           + System.currentTimeMillis();
                        configCenter.putAndGrant(configKey, JSON.toJSONString(cache), 30);
                        cache.clear();
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public void publish(String appName, List<KeyCountModel> models) {
        try {
            KeyCountItem countItem = new KeyCountItem(appName, models.get(0).getCreateTime(), models);
            counterQueue.put(countItem);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
