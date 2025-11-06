package com.cloud.arch.aggregate;

import java.io.Serializable;
import java.util.Optional;

/**
 * 聚合仓储接口
 *
 * @param <I> 聚合根Id
 * @param <R> 聚合根对象
 */
public interface Repository<I extends Serializable, R extends AggregateRoot<I>> {

    /**
     * 保存聚合根
     *
     * @param aggregate 聚合根
     */
    void save(Aggregate<I, R> aggregate);

    /**
     * 根据聚合标识查询聚合根
     *
     * @param id 聚合标识
     */
    default Optional<Aggregate<I, R>> ofNullable(I id) {
        return Optional.empty();
    }

    /**
     * 根据聚合标识查询聚合根
     *
     * @param id 聚合标识
     */
    default Aggregate<I, R> of(I id) {
        return null;
    }

}
