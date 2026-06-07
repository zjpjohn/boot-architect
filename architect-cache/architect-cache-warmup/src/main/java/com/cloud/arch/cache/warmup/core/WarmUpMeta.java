package com.cloud.arch.cache.warmup.core;

import lombok.Data;

import java.util.List;

/**
 * 缓存预热元数据视图，描述一个缓存名的可预热方法及示例参数
 */
@Data
public class WarmUpMeta {
    private String           cacheName;
    private String           remark;
    private List<MethodMeta> methods;
    private List<Object[]>   sampleArgs;
}
