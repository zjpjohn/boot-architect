package com.cloud.arch.cache.warmup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存预热配置属性，前缀 com.cloud.cache.warm-up
 */
@Data
@ConfigurationProperties(prefix = "com.cloud.cache.warm-up")
public class WarmUpProperties {

    /**
     * 是否启用自动预热
     */
    private boolean enabled = true;

    /**
     * 预热失败是否阻止应用启动
     */
    private boolean failFast = false;

    /**
     * 是否全局异步执行
     */
    private boolean async = false;

    /**
     * 分布式锁等待超时（秒）
     */
    private long lockWaitSeconds = 30;

    /**
     * 要执行的缓存名，空 = 全部执行
     */
    private Set<String> caches = new LinkedHashSet<>();

    /**
     * 是否暴露 /actuator/warmup 端点
     */
    private boolean restEndpoint = true;

    /**
     * 缓存名 → 预热参数映射
     */
    private Map<String, TaskConfig> tasks = new LinkedHashMap<>();

    @Data
    public static class TaskConfig {
        private Boolean            async;
        private Long               timeout;
        private List<List<Object>> args = new ArrayList<>();
    }
}
