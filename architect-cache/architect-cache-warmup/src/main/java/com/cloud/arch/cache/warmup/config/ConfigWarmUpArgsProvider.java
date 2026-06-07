package com.cloud.arch.cache.warmup.config;

import com.cloud.arch.cache.warmup.core.WarmUpArgsProvider;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 YAML 配置的预热参数提供者，从 WarmUpProperties 读取 args
 */
public class ConfigWarmUpArgsProvider implements WarmUpArgsProvider {

    private final WarmUpProperties properties;

    public ConfigWarmUpArgsProvider(WarmUpProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Object[]> provide(String cacheName) {
        WarmUpProperties.TaskConfig task = properties.getTasks().get(cacheName);
        if (task == null || CollectionUtils.isEmpty(task.getArgs())) {
            return Collections.emptyList();
        }
        return task.getArgs().stream().map(List::toArray).collect(Collectors.toList());
    }
}
