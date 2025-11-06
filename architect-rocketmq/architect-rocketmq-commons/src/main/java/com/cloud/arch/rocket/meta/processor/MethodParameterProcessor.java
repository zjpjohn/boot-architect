package com.cloud.arch.rocket.meta.processor;


import com.cloud.arch.rocket.meta.SenderMetadata;

import java.lang.annotation.Annotation;

public interface MethodParameterProcessor {

    /**
     * 参数处理器构建元数据
     *
     * @param metadata 元数据
     * @param type     参数类型
     * @param index    参数位置
     */
    default void buildMeta(SenderMetadata metadata, Class<?> type, int index) {
    }

    /**
     * 注解参数处理器构建元数据
     *
     * @param metadata   元数据
     * @param type       参数类型
     * @param index      参数位置
     * @param annotation 注解类型
     */
    default void buildMeta(SenderMetadata metadata, Class<?> type, int index, Annotation annotation) {
    }

}
