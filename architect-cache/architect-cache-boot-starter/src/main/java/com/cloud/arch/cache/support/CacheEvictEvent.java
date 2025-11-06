package com.cloud.arch.cache.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvictEvent {

    /**
     * 缓存名称
     */
    private String  name;
    /**
     * 缓存键值key
     */
    private Object  key;
    /**
     * 是否延迟删除
     */
    private boolean delayEvict;
    /**
     * 是否清除全部数据
     */
    private boolean allEntries = false;

    /**
     * 是否清除全部缓存值
     */
    public boolean isEvictAll() {
        return this.allEntries || Objects.isNull(this.key);
    }

    public CacheEvictEvent(String name, Object key) {
        this(name, key, true, false);
    }

    public CacheEvictEvent(String name, Object key, boolean delayEvict) {
        this(name, key, delayEvict, false);
    }

    public CacheEvictEvent(String name) {
        this(name, null, true, true);
    }

    public CacheEvictEvent(String name, boolean delayEvict) {
        this(name, null, delayEvict, true);
    }

    @Override
    public String toString() {
        return "CacheEvictEvent{"
               + "name='"
               + name
               + '\''
               + ", key="
               + key
               + ", delayEvict="
               + delayEvict
               + ", allEntries="
               + allEntries
               + '}';
    }
}
