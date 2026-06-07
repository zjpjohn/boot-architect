package com.cloud.arch.cache.warmup.core;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * 预热任务 POJO，描述一个可预热的方法及其所属缓存
 */
@Data
public class WarmUpTask {
    private String   cacheName;
    private String[] cacheNames;
    private String   beanName;
    private Object   targetBean;
    private Method   method;
}
