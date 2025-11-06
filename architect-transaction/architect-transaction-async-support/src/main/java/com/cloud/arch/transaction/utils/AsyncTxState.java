package com.cloud.arch.transaction.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AsyncTxState {

    /**
     * 任务等待状态
     */
    public static final int READY   = 1;
    /**
     * 任务正在执行状态
     */
    public static final int RUNNING = 2;
    /**
     * 任务执行成功状态
     */
    public static final int SUCCESS = 3;
    /**
     * 任务执行失败状态
     */
    public static final int FAIL    = 4;
    /**
     * 死信任务状态
     */
    public static final int DEAD    = 5;

}
