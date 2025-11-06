package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.CacheNodePolicy;
import com.cloud.arch.cache.core.RefreshEvent;

public class RandomNodePolicy implements CacheNodePolicy {

    private static final long NODE = RefreshEvent.randomNo();

    /**
     * 缓存节点生成
     */
    @Override
    public Long getCacheNode() {
        return NODE;
    }
}
