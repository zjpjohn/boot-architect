package com.cloud.arch.cache.support;


import com.cloud.arch.cache.core.RefreshEvent;
import com.cloud.arch.cache.core.RefreshPolicy;

public class DefaultRefreshPolicy implements RefreshPolicy {

    /**
     * 发布刷新缓存事件
     *
     * @param event 缓存事件
     */
    @Override
    public void publish(RefreshEvent event) {

    }

    /**
     * 获取刷新节点编号，判断是否为本地时间
     */
    @Override
    public long getRefreshNode() {
        return 0L;
    }

}
