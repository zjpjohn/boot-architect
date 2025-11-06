package com.cloud.arch.cache.config;

import com.cloud.arch.cache.core.RemoteCacheTtlRefresher;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.cache")
public class CloudCacheProperties {

    /**
     * 缓存操作刷新本地缓存topic
     */
    private String  refreshTopic;
    /**
     * 二级缓存过期时间刷新时间间隔
     * 默认-60秒
     */
    private Long    ttlRefreshInterval = RemoteCacheTtlRefresher.DEFAULT_REFRESH_INTERVAL;
    /**
     * 缓存延迟删除时间间隔,时间单位毫秒，默认-500毫秒
     */
    private Long    delayEvictInterval = 500L;
    /**
     * 是否开启缓存延迟双删，默认-开启
     */
    private boolean enableDelayEvict   = true;
    /**
     * 是否开启监控指标收集，默认-关闭
     */
    private boolean enableMetric       = false;
    /**
     * 是否开启Null值缓存，默认-开启
     */
    private boolean allowNullValue     = true;
    /**
     * 是否开启本地缓存，默认-开启
     */
    private boolean enableLocal        = true;
    /**
     * 缓存是否只对public方法生效
     */
    private boolean onlyPublic         = true;

}
