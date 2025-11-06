package com.cloud.arch.rocket.meta.processor.impl;

import com.cloud.arch.rocket.annotations.Delay;
import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.processor.MethodParameterProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class DelayAnnotationParameterProcessor implements MethodParameterProcessor {

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
        Assert.isNull(metadata.getDelay(), String.format("方法%s.%s中存在多个延迟时间参数.", metadata.getDeclareClassName(), metadata.getMethodName()));
        Delay                        delay = (Delay) annotation;
        SenderMetadata.DelayMetadata meta  = new SenderMetadata.DelayMetadata(index, delay.deliver(), delay.timeUnit());
        Assert.isTrue(checkDelay(meta, metadata.getMethod(), type, index), String.format("方法%s.%s第%d个参数必须为Long或者Collection<Long>类型", metadata.getMethod()
                                                                                                                                                          .getDeclaringClass()
                                                                                                                                                          .getSimpleName(), metadata.getMethod()
                                                                                                                                                                                    .getName(), index));
        metadata.getProcessors().put(index, this);
        metadata.setDelay(meta);
    }

    /**
     * 延迟消息必须为Long或者Collection<Long>类型判断
     *
     * @param meta  元数据信息
     * @param type  参数类型
     * @param index 参数标志位
     */
    private boolean checkDelay(SenderMetadata.DelayMetadata meta, Method method, Class<?> type, int index) {
        //延迟时间为Long
        if (ClassUtils.isAssignable(Long.class, type)) {
            meta.setCollection(false);
            return true;
        }
        Type parameterType = method.getGenericParameterTypes()[index];
        if (!(parameterType instanceof ParameterizedType)) {
            return false;
        }
        Type[] actualTypeArguments = ((ParameterizedType) parameterType).getActualTypeArguments();
        boolean checked = ClassUtils.isAssignable(Collection.class, type)
                          && actualTypeArguments.length == 1
                          && TypeUtils.isAssignable(Long.class, actualTypeArguments[0]);
        if (checked) {
            meta.setCollection(true);
        }
        return checked;
    }

}
