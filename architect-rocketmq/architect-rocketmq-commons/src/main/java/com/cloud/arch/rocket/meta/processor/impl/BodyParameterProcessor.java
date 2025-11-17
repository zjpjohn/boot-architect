package com.cloud.arch.rocket.meta.processor.impl;

import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.processor.MethodParameterProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class BodyParameterProcessor implements MethodParameterProcessor {

    /**
     * 注解参数处理器构建元数据
     *
     * @param metadata 元数据
     * @param type     参数类型
     * @param index    参数位置
     */
    @Override
    public void buildMeta(SenderMetadata metadata, Class<?> type, int index) {
        Assert.isNull(metadata.getPayload(),
                      String.format("方法%s.%s有多个消息体参数",
                                    metadata.getDeclareClassName(),
                                    metadata.getMethodName()));
        Assert.isTrue(checkBody(metadata, type, index),
                      String.format("方法%s.%s第%d个参数作为消息内容必须为Serializable或者Collection<Serializable>类型",
                                    metadata.getMethod().getDeclaringClass().getSimpleName(),
                                    metadata.getMethod().getName(),
                                    index));
        metadata.getProcessors().put(index, this);
        metadata.setPayload(index);
    }

    /**
     * 校验消息体类型
     *
     * @param metadata 元数据
     * @param type     消息体类型
     * @param index    消息体参数位置
     */
    private boolean checkBody(SenderMetadata metadata, Class<?> type, Integer index) {
        if (!metadata.getAnnotation().batch()) {
            //非批量消息 消息体类型应实现Serializable接口
            return ClassUtils.isAssignable(Serializable.class, type);
        }
        //批量消息消息体内容应为Collection<? extends Serializable>
        Type parameterType = metadata.getMethod().getGenericParameterTypes()[index];
        if (!(parameterType instanceof ParameterizedType)) {
            return false;
        }
        Type[] actualTypeArguments = ((ParameterizedType) parameterType).getActualTypeArguments();
        return ClassUtils.isAssignable(Collection.class, type)
                && actualTypeArguments.length == 1
                && TypeUtils.isAssignable(Serializable.class, actualTypeArguments[0]);
    }

}
