package com.cloud.arch.aggregate;

import java.io.Serializable;

/**
 * 聚合根标记
 *
 */
public interface AggregateRoot<I extends Serializable> extends Entity<I> {

}
