package com.cloud.arch.cache.core;

import java.security.SecureRandom;

public class RefreshEvent {

    public static final byte LOAD_CACHE = 0x01; // 加载数据到本地缓存
    public static final byte EVICT_KEY = 0x02; // 删除缓存数据
    public static final byte CLEAR_KEY = 0x03; // 清除缓存全部数据

    private byte action; // 事件动作
    private long node; // 发送事件节点标识
    private String name; // 缓存实例名称
    private Object key; // 缓存对象key

    public RefreshEvent() {}

    public RefreshEvent(byte action, long node, String name, Object key) {
        this.action = action;
        this.key = key;
        this.node = node;
        this.name = name;
    }

    public static RefreshEvent evict(long node, String name, Object key) {
        return new RefreshEvent(EVICT_KEY, node, name, key);
    }

    public static RefreshEvent clear(long node, String name) {
        return new RefreshEvent(CLEAR_KEY, node, name, null);
    }

    public static Long randomNo() {
        long ct = System.currentTimeMillis();
        SecureRandom rndSeed = new SecureRandom();
        return (rndSeed.nextInt(10000) * 1000 + ct % 1000);
    }

    public long getNode() {
        return node;
    }

    public byte getAction() {
        return action;
    }

    public Object getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "RefreshEvent{" + "action=" + action + ", node=" + node + ", name='" + name + '\'' + ", key=" + key
            + '}';
    }
}
