package com.cloud.arch.cache.core;

import com.cloud.arch.cache.annotations.Local;
import com.cloud.arch.cache.annotations.Remote;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CacheSettings {
    /**
     * 缓存过期时间
     */
    private long               expire;
    /**
     * 缓存刷新时间
     */
    private long               preloadTime;
    /**
     * 是否允许刷新
     */
    private boolean            enableRefresh;
    /**
     * Null缓存过期时间比例,Null值缓存时间expire/magnification
     */
    private int                magnification;
    /**
     * 是否允许Null缓存
     */
    private boolean            allowNullValue;
    /**
     * 缓存时间随机值上限
     */
    private int                randomBound;
    /**
     * 是否开启本地缓存
     */
    private boolean            enableLocal;
    /**
     * 本地缓存配置
     */
    private LocalCacheSettings local;

    /**
     * 构建缓存配置信息
     *
     * @param enableLocal 是否开启本地缓存
     * @param remote      L2缓存注解
     * @param local       L1缓存注解
     */
    public static CacheSettings build(boolean enableLocal, boolean allowNullValue, Remote remote, Local local) {
        return CacheSettings.builder().expire(remote.expire()).enableLocal(enableLocal).allowNullValue(allowNullValue)
                            .preloadTime(remote.preloadTime()).randomBound(remote.randomBound())
                            .magnification(remote.magnification()).enableRefresh(remote.enableRefresh())
                            .local(LocalCacheSettings.build(local)).build();
    }

    @Override
    public String toString() {
        return "CacheSettings{"
               + "expire="
               + expire
               + ", preloadTime="
               + preloadTime
               + ", enableRefresh="
               + enableRefresh
               + ", magnification="
               + magnification
               + ", allowNullValue="
               + allowNullValue
               + ", randomBound="
               + randomBound
               + '}';
    }

}

