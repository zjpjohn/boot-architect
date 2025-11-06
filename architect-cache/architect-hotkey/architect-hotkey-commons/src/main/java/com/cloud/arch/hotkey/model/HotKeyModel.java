package com.cloud.arch.hotkey.model;


import com.cloud.arch.hotkey.enums.KeyType;
import com.cloud.arch.utils.IdWorker;

import java.util.concurrent.atomic.LongAdder;

public class HotKeyModel {
    /**
     * 唯一标识
     */
    private String    id    = String.valueOf(IdWorker.nextId());
    /**
     * 创建时间
     */
    private Long      gmtCreate;
    /**
     * key标识
     */
    private String    key;
    /**
     * key对应的分组集合
     */
    private String    cache;
    /**
     * key出现次数,LongAdder解决并发问题
     */
    //    @JSONField(serializeUsing = LongAdderSerializer.class)
    private LongAdder count = new LongAdder();
    /**
     * 应用名称
     */
    private String    appName;
    /**
     * key类型
     */
    private KeyType   keyType;
    /**
     * 是否删除事件
     */
    private boolean   remove;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public void addCount(Long count) {
        this.count.add(count);
    }

    public long getCount() {
        return count.sum();
    }

    public void setCount(long count) {
        this.count.add(count);
    }

}
