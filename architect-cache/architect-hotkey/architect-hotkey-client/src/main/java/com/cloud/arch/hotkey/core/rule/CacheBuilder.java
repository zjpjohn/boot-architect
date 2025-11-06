package com.cloud.arch.hotkey.core.rule;

import com.cloud.arch.hotkey.rule.KeyRule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class CacheBuilder {

    /**
     * 构建缓存
     *
     * @param rule 热key缓存配置
     */
    public static Cache<String, Object> cache(KeyRule rule) {
        return cache(rule.getMinimum(), rule.getMaximum(), rule.getDuration());
    }

    /**
     * 构建caffeine缓存
     *
     * @param minSize 初始容量
     * @param maxSize 最大容量
     * @param expire  写入后多久过期
     */
    public static Cache<String, Object> cache(int minSize, int maxSize, int expire) {
        return Caffeine.newBuilder().initialCapacity(minSize).maximumSize(maxSize)
                       .expireAfterWrite(expire, TimeUnit.SECONDS).build();
    }
}
