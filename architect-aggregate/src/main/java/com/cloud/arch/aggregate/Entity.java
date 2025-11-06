package com.cloud.arch.aggregate;

import java.io.Serializable;

/**
 * 领域实体接口
 *
 * @param <I> 领域实体唯一标识
 */
public interface Entity<I extends Serializable> extends Serializable {

    /**
     * 设置实体标识
     */
    void setId(I id);

    /**
     * 获取实体表示
     */
    I getId();

}
