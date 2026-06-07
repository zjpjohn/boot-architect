package com.cloud.arch.cache.warmup.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** 汇总所有缓存名的元数据视图，供查询接口使用 */
    public List<WarmUpMeta> getAllMetas(WarmUpArgsProvider argsProvider) {
        return tasksByCache.entrySet().stream()
                .map(entry -> {
                    String           cacheName = entry.getKey();
                    List<WarmUpTask> tasks     = entry.getValue();
                    WarmUpMeta       meta      = new WarmUpMeta();
                    meta.setCacheName(cacheName);
                    meta.setRemark(tasks.get(0).getRemark());
                    meta.setSampleArgs(argsProvider.provide(cacheName));
                    meta.setMethods(tasks.stream()
                            .map(this::toMethodMeta)
                            .collect(Collectors.toList()));
                    return meta;
                })
                .collect(Collectors.toList());
    }

    private String[] toParamTypeNames(WarmUpTask task) {
        Class<?>[] types = task.getMethod().getParameterTypes();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getSimpleName();
        }
        return names;
    }

    /** 将 WarmUpTask 转为 MethodMeta 视图，供 WarmUpTemplate 复用 */
    MethodMeta toMethodMeta(WarmUpTask task) {
        MethodMeta mm = new MethodMeta();
        mm.setBeanName(task.getBeanName());
        mm.setMethodName(task.getMethod().getName());
        mm.setParamTypes(toParamTypeNames(task));
        return mm;
    }
}
