package com.cloud.arch.executor;

public interface Executor<K extends Comparable<K>> {

    /**
     * 业务执行器标识
     */
    K bizIndex();

}
