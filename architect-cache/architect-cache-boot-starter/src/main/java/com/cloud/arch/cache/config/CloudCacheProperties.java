package com.cloud.arch.cache.config;

import com.cloud.arch.cache.core.RemoteCacheTtlRefresher;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.cache")
public class CloudCacheProperties {

    /**
     * 缓存操作刷新本地缓存topic，默认值：cache:refresh:default
     */
    private String  refreshTopic       = "cache:refresh:default";
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
     * 延迟删除队列最大容量，超出后丢弃新任务并记录 warn 日志，默认-10000
     */
    private int     maxDelayEvictSize  = 10000;
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
    /**
     * L1 本地缓存最大 TTL 秒数，超期强制逐出防止 Pub/Sub 丢消息导致脏数据永不过期。
     * 默认 0 表示不限制（沿用缓存实例自身的 TTL 配置）。
     */
    private int     maxLocalTtlSeconds = 0;
    /**
     * 缓存刷新策略：pubsub（默认）| stream
     */
    private String  refreshType              = "pubsub";
    /**
     * Stream 裁剪最大长度，默认 10000
     */
    private int     refreshStreamMaxLen      = 10000;
    /**
     * Stream poll 批量大小，默认 100
     */
    private int     refreshStreamBatchSize   = 100;
    /**
     * Stream XREAD BLOCK 超时(ms)，默认 5000
     */
    private long    refreshStreamBlockTimeoutMs = 5000;

}
