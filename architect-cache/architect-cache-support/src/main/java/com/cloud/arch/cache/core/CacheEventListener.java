package com.cloud.arch.cache.core;

public interface CacheEventListener {

    /**
     * 初始化事件监听器
     */
    void initialize();

    /**
     * 缓存操作事件监听器
     *
     * @param event 缓存操作事件
     */
    void onEvent(RefreshEvent event);

    /**
     * 缓存事件监听节点编号
     */
    Long getLocalNode();

    /**
     * 判断事件是否为本地事件
     */
    default boolean isLocalEvent(RefreshEvent event) {
        return event.getNode() == getLocalNode();
    }

    /**
     * 销毁监听器，关闭消费者/轮询线程等资源。
     * 默认空实现，子类按需覆盖。
     */
    default void destroy() {
    }

}
