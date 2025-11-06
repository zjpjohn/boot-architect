package com.cloud.arch.aggregate;

import java.io.Serializable;

public interface AggregateRoot<I extends Serializable> extends Entity<I> {

    /**
     * 新创建版本
     */
    Integer NEW_VERSION = 0;

    /**
     * 查询聚合数据版本
     */
    default Integer getVersion() {
        return NEW_VERSION;
    }

    /**
     * 设置聚合数据版本
     */
    default void setVersion(Integer version) {
    }

    /**
     * 是否为新创建实体
     */
    default boolean isNew() {
        return NEW_VERSION.equals(this.getVersion());
    }

}
