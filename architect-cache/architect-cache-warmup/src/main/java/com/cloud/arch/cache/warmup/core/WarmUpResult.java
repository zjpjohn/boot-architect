package com.cloud.arch.cache.warmup.core;

import lombok.Data;

/**
 * 预热结果 POJO，记录单次预热操作的执行摘要
 */
@Data
public class WarmUpResult {
    private String  cacheName;
    private String  beanName;
    private String  methodName;
    private boolean success;
    private long    durationMs;
    private String  errorMessage;
    private int     totalCount;
    private int     successCount;
}
