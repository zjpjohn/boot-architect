package com.cloud.arch.hotkey.cache;

import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheBuilder {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * 构建所有来的要缓存的key cache
     */
    public static Cache<String, Object> buildAllKeyCache(HotKeyProperties properties) {
        return Caffeine.newBuilder().initialCapacity(8192)//初始大小
                       .maximumSize(5000000)//最大数量。这个数值我设置的很大，按30万每秒，每分钟是1800万，实际可以调小
                       .expireAfterWrite(properties.getCacheTimeout(), TimeUnit.SECONDS)//过期时间，默认60秒
                       .executor(executorService).softValues().build();
    }

    /**
     * 刚生成的热key，先放这里放几秒后，应该所有客户端都收到了热key并本地缓存了。这几秒内，不再处理同样的key了
     */
    public static Cache<String, Object> buildRecentHotKeyCache() {
        return Caffeine.newBuilder().initialCapacity(256)//初始大小
                       .maximumSize(50000)//最大数量
                       .expireAfterWrite(5, TimeUnit.SECONDS)//过期时间
                       .executor(executorService).softValues().build();
    }

}
