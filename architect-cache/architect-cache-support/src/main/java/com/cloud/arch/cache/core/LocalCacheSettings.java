package com.cloud.arch.cache.core;

import com.cloud.arch.cache.annotations.Local;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocalCacheSettings {

    //本地缓存初始容量
    private int        initialSize;
    //本地缓存最大容量
    private int        maximumSize;
    //本地缓存有效时间
    private long       expireTime;
    //缓存失效模式
    private ExpireMode expireMode;

    /**
     * L1缓存配置信息
     */
    public static LocalCacheSettings build(Local local) {
        return LocalCacheSettings.builder().expireMode(local.expireMode()).expireTime(local.expire())
                                 .initialSize(local.initialSize()).maximumSize(local.maximumSize()).build();
    }

    @Override
    public String toString() {
        return "LocalCacheProperties{"
               + "initialSize="
               + initialSize
               + ", maximumSize="
               + maximumSize
               + ", expireTime="
               + expireTime
               + ", expireMode="
               + expireMode
               + '}';
    }

}
