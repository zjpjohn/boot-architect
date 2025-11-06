package com.cloud.arch.cache.core;


import com.cloud.arch.cache.metrics.StatsManager;

import java.util.Collection;

public interface CacheManager {

    /**
     * 缓存监控指标管理器
     */
    default StatsManager statsManager() {
        return StatsManager.disabledManager();
    }

    /**
     * 查询指定名称L2缓存实例
     *
     * @param name 缓存名称
     */
    Cache getCache(String name);

    /**
     * 动态激活本地缓存
     *
     * @param name 缓存名称
     */
    void activateLocal(String name);

    /**
     * 卸载本地缓存
     *
     * @param name 缓存名称
     */
    void detachLocal(String name);

    /**
     * 获取并添加L2缓存
     *
     * @param name     缓存名称
     * @param settings 二级缓存名称
     */
    Cache getAndAdd(String name, CacheSettings settings);

    /**
     * 获取全部L2缓存名称
     */
    Collection<String> getCacheNames();

}
