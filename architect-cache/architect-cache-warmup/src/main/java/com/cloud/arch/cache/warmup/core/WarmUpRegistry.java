package com.cloud.arch.cache.warmup.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预热任务注册表，按缓存名索引所有已扫描的预热任务
 */
public class WarmUpRegistry {

    private final Map<String, List<WarmUpTask>> tasksByCache = new HashMap<>();

    public void register(String cacheName, WarmUpTask task) {
        tasksByCache.computeIfAbsent(cacheName, k -> new ArrayList<>()).add(task);
    }

    public List<WarmUpTask> getTasksByCache(String cacheName) {
        return Collections.unmodifiableList(
                tasksByCache.getOrDefault(cacheName, Collections.emptyList()));
    }

    public Set<String> getCacheNames() {
        return Collections.unmodifiableSet(tasksByCache.keySet());
    }

    public int cacheNameCount() {
        return tasksByCache.size();
    }
}
