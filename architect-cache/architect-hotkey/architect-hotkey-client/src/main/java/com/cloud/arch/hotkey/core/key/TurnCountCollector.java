package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class TurnCountCollector implements IKeyCollector<KeyHotModel, KeyCountModel> {

    //时间格式化
    private static final String FORMAT                    = "yyyy-MM-dd HH:mm:ss";
    //并行计算开启阈值
    private static final int    PARALLEL_SWITCH_THRESHOLD = 5000;

    private final ConcurrentMap<String, HitCount> cache0 = Maps.newConcurrentMap();
    private final ConcurrentMap<String, HitCount> cache1 = Maps.newConcurrentMap();
    private final AtomicLong                      cursor = new AtomicLong(0);

    public TurnCountCollector() {
    }

    /**
     * 锁定后返回数据
     */
    @Override
    public List<KeyCountModel> lockAndGet() {
        cursor.addAndGet(1);
        ConcurrentMap<String, HitCount> reader = cursor.get() % 2 == 0 ? cache1 : cache0;
        List<KeyCountModel> result = StreamSupport.stream(reader.entrySet().spliterator(),
                        reader.size() > PARALLEL_SWITCH_THRESHOLD)
                .map(entry -> {
                    HitCount      hitCount = entry.getValue();
                    KeyCountModel model    = new KeyCountModel();
                    model.setRuleKey(entry.getKey());
                    model.setHotHitCount((int) hitCount.hotHitCount.sum());
                    model.setTotalHitCount((int) hitCount.totalHitCount.sum());
                    return model;
                }).collect(Collectors.toList());
        reader.clear();
        return result;
    }

    /**
     * 收集输入的参数
     */
    @Override
    public void collect(KeyHotModel model) {
        //格式化当前时间到秒
        String nowTime = DateTimeFormatter.ofPattern(FORMAT).format(LocalDateTime.now());
        //cache+分隔符+nowTime
        String mapKey = model.getCache() + HotkeyConstants.COUNT_DELIMITER + nowTime;
        //cache读写分离
        ConcurrentMap<String, HitCount> map      = cursor.get() % 2 == 0 ? cache0 : cache1;
        HitCount                        hitCount = map.computeIfAbsent(mapKey, v -> new HitCount());
        hitCount.totalHitCount.increment();
        if (model.isHot()) {
            hitCount.hotHitCount.increment();
        }
    }

    @Override
    public void finishOnce() {

    }

    private static class HitCount {
        private final LongAdder hotHitCount   = new LongAdder();
        private final LongAdder totalHitCount = new LongAdder();
    }

}
