package com.cloud.arch.aggregate;

import java.io.Serializable;

/**
 * 领域实体接口
 *
 * @param <I> 领域实体唯一标识
 */
public interface Entity<I extends Serializable> extends Serializable {

    /**
     * 新创建版本
     */
    Integer NEW_VERSION = 0;

    /**
     * 设置实体标识
     */
    void setId(I id);

    /**
     * 获取实体表示
     */
    I getId();

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
