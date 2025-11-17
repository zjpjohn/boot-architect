package com.cloud.arch.rocket.meta.processor.impl;

import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.processor.MethodParameterProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;


public class ShardingKeyAnnotationParameterProcessor implements MethodParameterProcessor {

    /**
     * 注解参数处理器构建元数据
     *
     * @param metadata   元数据
     * @param type       参数类型
     * @param index      参数位置
     * @param annotation 注解类型
     */
    @Override
    public void buildMeta(SenderMetadata metadata, Class<?> type, int index, Annotation annotation) {
        Assert.isNull(metadata.getShardingKey(),
                      String.format("方法%s.%s中存在多个shardingKey参数.",
                                    metadata.getDeclareClassName(),
                                    metadata.getMethodName()));
        Assert.isTrue(ClassUtils.isAssignable(String.class, type),
                      String.format("方法%s.%s第%d个参数作为shardingKey必须为String类型",
                                    metadata.getDeclareClassName(),
                                    metadata.getMethodName(),
                                    index));
        metadata.setShardingKey(index);
    }
}
