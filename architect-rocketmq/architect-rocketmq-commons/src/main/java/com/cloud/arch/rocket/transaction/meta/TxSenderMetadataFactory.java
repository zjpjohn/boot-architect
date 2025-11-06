package com.cloud.arch.rocket.transaction.meta;

import com.cloud.arch.rocket.annotations.TxSender;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TxSenderMetadataFactory {

    private static final Map<AnnotatedElementKey, TxSenderMetadata> metadataCache = Maps.newConcurrentMap();

    public static List<TxSenderMetadata> getMetadataList() {
        return Lists.newArrayList(metadataCache.values());
    }

    /**
     * 获取指定方法的事物消息元数据
     */
    public static TxSenderMetadata getTxSenderMeta(Class<?> targetClass, Method method) {
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        return metadataCache.get(elementKey);
    }

    /**
     * 获取调用方法的事物消息元数据
     */
    public static TxSenderMetadata getTxSenderMeta(MethodInvocation invocation) {
        Object              candidate   = Objects.requireNonNull(invocation.getThis());
        Class<?>            targetClass = AopUtils.getTargetClass(candidate);
        Method              method      = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        AnnotatedElementKey elementKey  = new AnnotatedElementKey(method, targetClass);
        return metadataCache.get(elementKey);
    }

    /**
     * 解析类中方法事物消息元数据
     *
     * @param clazz 指定类
     */
    public static boolean hasTxSenderAnnotation(Class<?> clazz) {
        List<TxSenderMetadata> metadatas = Lists.newArrayList();
        ReflectionUtils.doWithMethods(clazz, method -> {
            TxSender annotation = method.getAnnotation(TxSender.class);
            if (annotation != null) {
                TxSenderMetadata    metadata   = new TxSenderMetadata(clazz, method, annotation);
                AnnotatedElementKey elementKey = new AnnotatedElementKey(method, metadata.getTargetClass());
                metadataCache.put(elementKey, metadata);
                metadatas.add(metadata);
            }
        });
        return !CollectionUtils.isEmpty(metadatas);
    }

}
