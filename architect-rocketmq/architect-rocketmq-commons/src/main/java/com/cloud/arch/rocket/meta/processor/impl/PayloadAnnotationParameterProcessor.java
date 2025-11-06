package com.cloud.arch.rocket.meta.processor.impl;


import com.cloud.arch.rocket.meta.SenderMetadata;

import java.lang.annotation.Annotation;

public class PayloadAnnotationParameterProcessor extends BodyParameterProcessor {

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
        this.buildMeta(metadata, type, index);
    }

}
