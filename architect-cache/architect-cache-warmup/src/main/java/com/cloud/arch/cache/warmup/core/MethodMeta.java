package com.cloud.arch.cache.warmup.core;

import lombok.Data;

/**
 * 预热方法元数据，描述单个可预热方法的参数签名
 */
@Data
public class MethodMeta {
    private String   beanName;
    private String   methodName;
    private String[] paramTypes;
}
