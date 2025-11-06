package com.cloud.arch.cache.core;

public interface RefreshPolicy {

    /**
     * 发布刷新缓存事件
     *
     * @param event 缓存事件
     */
    void publish(RefreshEvent event);

    /**
     * 获取刷新节点编号，判断是否为本地时间
     */
    long getRefreshNode();


    /**
     * 发布淘汰缓存事件
     *
     * @param key 缓存key
     */
    default void sendEvict(String name, Object key) {
        publish(RefreshEvent.evict(getRefreshNode(), name, key));
    }

    /**
     * 发送清除缓存事件
     */
    default void sendClear(String name) {
        publish(RefreshEvent.clear(getRefreshNode(), name));
    }

}
